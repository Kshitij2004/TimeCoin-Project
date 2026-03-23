import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import Marketplace from "./Marketplace";
import api from "../../services/api.js";

// 1. Mock the centralized API service
jest.mock("../../services/api.js", () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
    post: jest.fn(),
  },
}));

// 2. Mock navigation (already present in your file)
jest.mock("react-router-dom", () => ({
  useNavigate: () => jest.fn(),
}), { virtual: true });

// ── Mock data ────────────────────────────────────────────────────────────
const mockCoin = {
  currentPrice: "10.00",
  circulatingSupply: "500000.00",
  totalSupply: "1000000.00",
};

describe("Marketplace — page renders", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Default success mock for coin data
    api.get.mockResolvedValue({ data: mockCoin });
  });

  it("shows the Marketplace heading", async () => {
    render(<Marketplace />);
    expect(await screen.findByText("Marketplace")).toBeInTheDocument();
  });

  it("shows the buy form", async () => {
    render(<Marketplace />);
    expect(await screen.findByTestId("amount-input")).toBeInTheDocument();
    expect(screen.getByTestId("buy-button")).toBeInTheDocument();
  });
});

describe("Marketplace — coin data display", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    api.get.mockResolvedValue({ data: mockCoin });
  });

  it("shows current price after loading", async () => {
    render(<Marketplace />);
    expect(await screen.findByTestId("coin-price")).toHaveTextContent("$10.00");
  });

  it("shows circulating supply", async () => {
    render(<Marketplace />);
    expect(await screen.findByTestId("circulating-supply")).toHaveTextContent("500,000");
  });
});

describe("Marketplace — buy form interactions", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    api.get.mockResolvedValue({ data: mockCoin });
  });

  it("shows estimated cost preview as user types", async () => {
    render(<Marketplace />);
    await screen.findByTestId("coin-price");

    const input = screen.getByTestId("amount-input");
    await userEvent.type(input, "5");
    expect(screen.getByTestId("cost-preview")).toHaveTextContent("$50.00");
  });

  it("shows validation error if amount is invalid", async () => {
    render(<Marketplace />);
    await screen.findByTestId("amount-input");

    const input = screen.getByTestId("amount-input");
    await userEvent.type(input, "-1");
    await userEvent.click(screen.getByTestId("buy-button"));
    
    expect(await screen.findByTestId("status-message")).toHaveTextContent("valid amount");
    // Verify API was never called because of client-side validation
    expect(api.post).not.toHaveBeenCalled();
  });
});

describe("Marketplace — successful purchase", () => {
  it("shows success message and clears input after buy", async () => {
    api.get.mockResolvedValue({ data: mockCoin });
    api.post.mockResolvedValue({ data: { message: "Purchase successful" } });

    render(<Marketplace />);
    await screen.findByTestId("coin-price");

    const input = screen.getByTestId("amount-input");
    await userEvent.type(input, "10");
    await userEvent.click(screen.getByTestId("buy-button"));

    expect(await screen.findByTestId("status-message")).toHaveTextContent("Successfully purchased");
    
    // Check if input cleared (Axios handles null/empty differently in tests)
    await waitFor(() => {
      expect(screen.getByTestId("amount-input")).toHaveValue(null);
    });
  });
});

describe("Marketplace — failed purchase", () => {
  it("shows API error message on failed buy", async () => {
    api.get.mockResolvedValue({ data: mockCoin });
    
    // Mock Axios error response
    api.post.mockRejectedValue({
      response: {
        data: { message: "Insufficient circulating supply" }
      }
    });

    render(<Marketplace />);
    await screen.findByTestId("coin-price");

    await userEvent.type(screen.getByTestId("amount-input"), "999");
    await userEvent.click(screen.getByTestId("buy-button"));

    expect(await screen.findByTestId("status-message")).toHaveTextContent(
      "Insufficient circulating supply"
    );
  });
});