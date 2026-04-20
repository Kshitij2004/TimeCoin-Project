import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BrowserRouter } from "react-router-dom";
import Marketplace from "./Marketplace.js";
import api from "../../services/api.js";

// 1. Mock the centralized API service
jest.mock("../../services/api.js", () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
    post: jest.fn(),
  },
}));

// 2. Mock navigation
const mockedUsedNavigate = jest.fn();
jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useNavigate: () => mockedUsedNavigate,
}));

// ── Mock data (Using explicit numbers to avoid parsing errors) ───────────
const mockCoin = {
  currentPrice: 10,
  circulatingSupply: 500000,
  totalSupply: 1000000,
};

const mockListings = [
  { id: 1, title: "Logo Design", price: 50, category: "Services", isSold: false },
  { id: 2, title: "Used Monitor", price: 100, category: "Goods", isSold: true }
];

// Helper to wrap component in Router for tests
const renderWithRouter = (ui) => {
  return render(
    <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      {ui}
    </BrowserRouter>
  );
};

describe("Marketplace — page renders", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    api.get.mockImplementation((url) => {
      if (url.includes("/coin")) return Promise.resolve({ data: mockCoin });
      if (url.includes("/listings")) return Promise.resolve({ data: mockListings });
      return Promise.reject(new Error("not found"));
    });
  });

  it("shows the Marketplace heading", async () => {
    renderWithRouter(<Marketplace />);
    expect(await screen.findByText("Marketplace")).toBeInTheDocument();
  });

  it("shows the buy form", async () => {
    renderWithRouter(<Marketplace />);
    expect(await screen.findByTestId("amount-input", {}, { timeout: 3000 })).toBeInTheDocument();
    expect(screen.getByTestId("buy-button")).toBeInTheDocument();
  });
});

describe("Marketplace — coin data display", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Using a very explicit mock implementation here to avoid any scope issues
    api.get.mockImplementation((url) => {
      if (url.includes("/coin")) {
        return Promise.resolve({
          data: {
            currentPrice: 10,
            circulatingSupply: 500000,
            totalSupply: 1000000
          }
        });
      }
      if (url.includes("/listings")) {
        return Promise.resolve({ data: [] });
      }
      return Promise.reject(new Error("not found"));
    });
  });

  it("shows current price after loading", async () => {
    renderWithRouter(<Marketplace />);
    // wait for the loading placeholder to be replaced with the real price
    await waitFor(() => {
      expect(screen.getByTestId("coin-price")).toHaveTextContent("$10.00");
    }, { timeout: 3000 });
  });

  it("shows balance/circulating supply", async () => {
    renderWithRouter(<Marketplace />);
    await waitFor(() => {
      expect(screen.getByTestId("circulating-supply")).toHaveTextContent("500,000");
    }, { timeout: 3000 });
  });
});

describe("Marketplace — buy form interactions", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    api.get.mockImplementation((url) => {
      if (url.includes("/coin")) return Promise.resolve({ data: mockCoin });
      if (url.includes("/listings")) return Promise.resolve({ data: [] });
      return Promise.reject(new Error("not found"));
    });
  });

  it("shows estimated cost preview as user types", async () => {
    renderWithRouter(<Marketplace />);
    const input = await screen.findByTestId("amount-input", {}, { timeout: 3000 });
    await userEvent.type(input, "5");
    expect(await screen.findByTestId("cost-preview")).toHaveTextContent("$50.00");
  });

  it("shows validation error if amount is invalid", async () => {
    renderWithRouter(<Marketplace />);
    const input = await screen.findByTestId("amount-input", {}, { timeout: 3000 });
    await userEvent.type(input, "-1");
    await userEvent.click(screen.getByTestId("buy-button"));
    expect(await screen.findByTestId("status-message")).toHaveTextContent("valid amount");
  });
});

describe("Marketplace — successful purchase", () => {
  it("shows success message and clears input after buy", async () => {
    api.get.mockImplementation((url) => {
      if (url.includes("/coin")) return Promise.resolve({ data: mockCoin });
      if (url.includes("/listings")) return Promise.resolve({ data: [] });
      return Promise.resolve({ data: mockCoin });
    });
    api.post.mockResolvedValue({ data: { message: "Purchase successful" } });

    renderWithRouter(<Marketplace />);
    const input = await screen.findByTestId("amount-input", {}, { timeout: 3000 });
    await userEvent.type(input, "10");
    await userEvent.click(screen.getByTestId("buy-button"));

    expect(await screen.findByTestId("status-message")).toHaveTextContent("Successfully purchased");

    await waitFor(() => {
      expect(screen.getByTestId("amount-input")).toHaveValue(null);
    });
  });
});