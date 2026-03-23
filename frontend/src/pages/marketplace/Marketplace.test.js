import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BrowserRouter } from "react-router-dom"; // Added for navigation context
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

// 2. Mock navigation (Properly mocked for React Router v6)
const mockedUsedNavigate = jest.fn();
jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useNavigate: () => mockedUsedNavigate,
}));

// ── Mock data ────────────────────────────────────────────────────────────
const mockCoin = {
  currentPrice: "10.00",
  circulatingSupply: "500000.00",
  totalSupply: "1000000.00",
};

const mockListings = [
  { id: 1, title: "Logo Design", price: 50, category: "Services", isSold: false },
  { id: 2, title: "Used Monitor", price: 100, category: "Goods", isSold: true }
];

// Helper to wrap component in Router for tests
const renderWithRouter = (ui) => {
  return render(<BrowserRouter>{ui}</BrowserRouter>);
};

describe("Marketplace — page renders", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Support both API calls used in Promise.all
    api.get.mockImplementation((url) => {
      if (url === "/coin") return Promise.resolve({ data: mockCoin });
      if (url === "/listings") return Promise.resolve({ data: mockListings });
      return Promise.reject(new Error("not found"));
    });
  });

  it("shows the Marketplace heading", async () => {
    renderWithRouter(<Marketplace />);
    expect(await screen.findByText("Marketplace")).toBeInTheDocument();
  });

  it("shows the buy form", async () => {
    renderWithRouter(<Marketplace />);
    // Wait for data to load so loading states disappear
    expect(await screen.findByTestId("amount-input")).toBeInTheDocument();
    expect(screen.getByTestId("buy-button")).toBeInTheDocument();
  });

  it("shows the listings grid", async () => {
    renderWithRouter(<Marketplace />);
    expect(await screen.findByText("Logo Design")).toBeInTheDocument();
    expect(screen.getByText("Used Monitor")).toBeInTheDocument();
  });
});

describe("Marketplace — coin data display", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    api.get.mockImplementation((url) => {
      if (url === "/coin") return Promise.resolve({ data: mockCoin });
      if (url === "/listings") return Promise.resolve({ data: [] });
      return Promise.reject(new Error("not found"));
    });
  });

  it("shows current price after loading", async () => {
    renderWithRouter(<Marketplace />);
    expect(await screen.findByTestId("coin-price")).toHaveTextContent("$10.00");
  });

  it("shows balance/circulating supply", async () => {
    renderWithRouter(<Marketplace />);
    // Updated to match your new "Your Balance" label in UI
    expect(await screen.findByText(/500,000/)).toBeInTheDocument();
  });
});

describe("Marketplace — buy form interactions", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    api.get.mockImplementation((url) => {
      if (url === "/coin") return Promise.resolve({ data: mockCoin });
      if (url === "/listings") return Promise.resolve({ data: [] });
      return Promise.reject(new Error("not found"));
    });
  });

  it("shows validation error if amount is invalid", async () => {
    renderWithRouter(<Marketplace />);
    const input = await screen.findByTestId("amount-input");
    
    await userEvent.type(input, "-1");
    await userEvent.click(screen.getByTestId("buy-button"));
    
    expect(await screen.findByTestId("status-message")).toHaveTextContent("valid amount");
    expect(api.post).not.toHaveBeenCalled();
  });
});

describe("Marketplace — successful purchase", () => {
  it("shows success message and clears input after buy", async () => {
    api.get.mockImplementation((url) => {
      if (url === "/coin") return Promise.resolve({ data: mockCoin });
      if (url === "/listings") return Promise.resolve({ data: [] });
      return Promise.resolve({ data: mockCoin }); // For refresh call
    });
    api.post.mockResolvedValue({ data: { message: "Purchase successful" } });

    renderWithRouter(<Marketplace />);
    const input = await screen.findByTestId("amount-input");

    await userEvent.type(input, "10");
    await userEvent.click(screen.getByTestId("buy-button"));

    expect(await screen.findByTestId("status-message")).toHaveTextContent("Successfully purchased");
    
    await waitFor(() => {
      expect(screen.getByTestId("amount-input")).toHaveValue(null);
    });
  });
});