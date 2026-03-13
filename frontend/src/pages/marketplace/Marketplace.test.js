// Marketplace.test.js
// Place at: src/pages/marketplace/Marketplace.test.js
//
// Setup (run once from project root):
//   npm install -D @testing-library/user-event @testing-library/jest-dom
//
// Make sure src/setupTests.js contains:
//   import '@testing-library/jest-dom';
//
// Run tests:
//   npm test

import { act, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import Marketplace from "./Marketplace";

jest.mock("react-router-dom", () => ({
  useNavigate: () => jest.fn(),
}), { virtual: true });

// ── Mock coin data ────────────────────────────────────────────────────────────
const mockCoin = {
  currentPrice: "10.00",
  circulatingSupply: "500000.00",
  totalSupply: "1000000.00",
};

// ── Router wrapper ────────────────────────────────────────────────────────────
function renderWithRouter(ui) {
  return render(ui);
}

// ── Fetch mock helper ─────────────────────────────────────────────────────────
function mockFetch(responses) {
  let callIndex = 0;
  global.fetch = jest.fn(() => {
    const resp = responses[callIndex] ?? responses[responses.length - 1];
    callIndex++;
    return Promise.resolve({
      ok: resp.ok ?? true,
      headers: { get: () => "application/json" },
      json: () => Promise.resolve(resp.json),
    });
  });
}

beforeEach(() => {
  jest.clearAllMocks();
});

// ─────────────────────────────────────────────────────────────────────────────
// Auth gate — skipped until real auth context is wired up
// ─────────────────────────────────────────────────────────────────────────────
describe.skip("Marketplace — auth gate (enable when auth is wired up)", () => {
  it("redirects to /login when user is not logged in", () => {
    mockFetch([{ json: mockCoin }]);
    renderWithRouter(<Marketplace />);
    expect(screen.getByTestId("login-page")).toBeInTheDocument();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Page renders
// ─────────────────────────────────────────────────────────────────────────────
describe("Marketplace — page renders", () => {
  it("shows the Marketplace heading", async () => {
    mockFetch([{ json: mockCoin }]);
    renderWithRouter(<Marketplace />);
    expect(await screen.findByText("Marketplace")).toBeInTheDocument();
  });

  it("shows the buy form", async () => {
    mockFetch([{ json: mockCoin }]);
    renderWithRouter(<Marketplace />);
    expect(await screen.findByTestId("amount-input")).toBeInTheDocument();
    expect(screen.getByTestId("buy-button")).toBeInTheDocument();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Coin data display
// ─────────────────────────────────────────────────────────────────────────────
describe("Marketplace — coin data display", () => {
  it("shows current price after loading", async () => {
    mockFetch([{ json: mockCoin }]);
    renderWithRouter(<Marketplace />);
    expect(await screen.findByTestId("coin-price")).toHaveTextContent("$10.00");
  });

  it("shows circulating supply", async () => {
    mockFetch([{ json: mockCoin }]);
    renderWithRouter(<Marketplace />);
    expect(await screen.findByTestId("circulating-supply")).toHaveTextContent("500,000");
  });

  it("shows total supply", async () => {
    mockFetch([{ json: mockCoin }]);
    renderWithRouter(<Marketplace />);
    expect(await screen.findByTestId("total-supply")).toHaveTextContent("1,000,000");
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Buy form interactions
// ─────────────────────────────────────────────────────────────────────────────
describe("Marketplace — buy form interactions", () => {
  it("shows estimated cost preview as user types", async () => {
    mockFetch([{ json: mockCoin }]);
    renderWithRouter(<Marketplace />);
    await screen.findByTestId("coin-price");

    await userEvent.type(screen.getByTestId("amount-input"), "5");
    expect(screen.getByTestId("cost-preview")).toHaveTextContent("$50.00");
  });

  it("shows validation error if amount is invalid", async () => {
    mockFetch([{ json: mockCoin }]);
    renderWithRouter(<Marketplace />);
    await screen.findByTestId("amount-input");

    await userEvent.type(screen.getByTestId("amount-input"), "-1");
    await userEvent.click(screen.getByTestId("buy-button"));
    expect(await screen.findByTestId("status-message")).toHaveTextContent("valid amount");
  });

  it("disables buy button while request is in flight", async () => {
    let resolveBuy;
    global.fetch = jest.fn()
      .mockResolvedValueOnce({
        ok: true,
        headers: { get: () => "application/json" },
        json: () => Promise.resolve(mockCoin),
      })
      .mockReturnValueOnce(
        new Promise((resolve) => {
          resolveBuy = () =>
            resolve({
              ok: true,
              headers: { get: () => "application/json" },
              json: () => Promise.resolve({ message: "ok" }),
            });
        })
      )
      .mockResolvedValueOnce({
        ok: true,
        headers: { get: () => "application/json" },
        json: () => Promise.resolve(mockCoin),
      });

    renderWithRouter(<Marketplace />);
    await screen.findByTestId("coin-price");

    await userEvent.type(screen.getByTestId("amount-input"), "5");
    await userEvent.click(screen.getByTestId("buy-button"));

    expect(screen.getByTestId("buy-button")).toBeDisabled();
    await act(async () => {
      resolveBuy();
    });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Successful purchase
// ─────────────────────────────────────────────────────────────────────────────
describe("Marketplace — successful purchase", () => {
  it("shows success message and clears input after buy", async () => {
    mockFetch([
      { json: mockCoin },
      { ok: true, json: { message: "Purchase successful" } },
      { json: mockCoin },
    ]);
    renderWithRouter(<Marketplace />);
    await screen.findByTestId("coin-price");

    await userEvent.type(screen.getByTestId("amount-input"), "10");
    await userEvent.click(screen.getByTestId("buy-button"));

    expect(await screen.findByTestId("status-message")).toHaveTextContent("Successfully purchased");
    expect(screen.getByTestId("amount-input")).toHaveValue(null);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Failed purchase
// ─────────────────────────────────────────────────────────────────────────────
describe("Marketplace — failed purchase", () => {
  it("shows API error message on failed buy", async () => {
    mockFetch([
      { json: mockCoin },
      { ok: false, json: { message: "Insufficient circulating supply" } },
    ]);
    renderWithRouter(<Marketplace />);
    await screen.findByTestId("coin-price");

    await userEvent.type(screen.getByTestId("amount-input"), "999999");
    await userEvent.click(screen.getByTestId("buy-button"));

    expect(await screen.findByTestId("status-message")).toHaveTextContent(
      "Insufficient circulating supply"
    );
  });
});
