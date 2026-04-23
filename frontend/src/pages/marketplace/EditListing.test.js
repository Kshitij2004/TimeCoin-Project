import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BrowserRouter } from "react-router-dom";
import EditListing from "./EditListing.js";
import api from "../../services/api.js";

jest.mock("../../services/api.js", () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
    put: jest.fn(),
  },
}));

const mockedUsedNavigate = jest.fn();
jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useNavigate: () => mockedUsedNavigate,
  useParams: () => ({ id: "42" }),
}));

const mockOwnedListing = {
  id: 42,
  title: "Logo Design",
  description: "Custom vector logo",
  price: 50,
  category: "Services",
  imageUrl: "https://example.com/logo.png",
  status: "ACTIVE",
  sellerId: 7,
};

const mockWallet = { userId: 7, walletAddress: "wlt_test" };

const renderWithRouter = (ui) =>
  render(
    <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      {ui}
    </BrowserRouter>
  );

describe("EditListing — auth guard", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
  });

  it("redirects to /login when no token is present", () => {
    renderWithRouter(<EditListing />);
    expect(mockedUsedNavigate).toHaveBeenCalledWith("/login", { replace: true });
  });
});

describe("EditListing — fetch + populate", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.setItem("token", "test-token");
    api.get.mockImplementation((url) => {
      if (url === "/listings/42") return Promise.resolve({ data: mockOwnedListing });
      if (url === "/wallet") return Promise.resolve({ data: mockWallet });
      return Promise.reject(new Error("unexpected URL: " + url));
    });
  });

  it("shows the edit heading after the fetch resolves", async () => {
    renderWithRouter(<EditListing />);
    expect(
      await screen.findByRole("heading", { level: 1, name: "Edit Listing" })
    ).toBeInTheDocument();
  });

  it("pre-populates the form with the listing's current values", async () => {
    renderWithRouter(<EditListing />);
    const titleInput = await screen.findByLabelText("Title");
    expect(titleInput).toHaveValue("Logo Design");
    expect(screen.getByLabelText("Description")).toHaveValue("Custom vector logo");
    expect(screen.getByLabelText("Price (TimeCoin)")).toHaveValue(50);
    expect(screen.getByLabelText("Category")).toHaveValue("Services");
    expect(screen.getByLabelText(/Image URL/)).toHaveValue("https://example.com/logo.png");
  });
});

describe("EditListing — fetch error", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.setItem("token", "test-token");
  });

  it("surfaces a message when the initial fetch fails", async () => {
    api.get.mockImplementation((url) => {
      if (url === "/listings/42") {
        return Promise.reject({ response: { data: { message: "Not found" } } });
      }
      if (url === "/wallet") return Promise.resolve({ data: mockWallet });
      return Promise.reject(new Error("unexpected URL"));
    });

    renderWithRouter(<EditListing />);
    expect(await screen.findByRole("alert")).toHaveTextContent("Not found");
  });
});

describe("EditListing — ownership guard", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.setItem("token", "test-token");
  });

  it("redirects non-owners back to the detail page", async () => {
    api.get.mockImplementation((url) => {
      if (url === "/listings/42") {
        return Promise.resolve({ data: { ...mockOwnedListing, sellerId: 999 } });
      }
      if (url === "/wallet") return Promise.resolve({ data: mockWallet });
      return Promise.reject(new Error("unexpected URL"));
    });

    renderWithRouter(<EditListing />);

    await waitFor(() => {
      expect(mockedUsedNavigate).toHaveBeenCalledWith("/marketplace/42", { replace: true });
    });
  });
});

describe("EditListing — validation", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.setItem("token", "test-token");
    api.get.mockImplementation((url) => {
      if (url === "/listings/42") return Promise.resolve({ data: mockOwnedListing });
      if (url === "/wallet") return Promise.resolve({ data: mockWallet });
      return Promise.reject(new Error("unexpected URL"));
    });
  });

  it("rejects empty required fields", async () => {
    renderWithRouter(<EditListing />);
    const titleInput = await screen.findByLabelText("Title");
    await userEvent.clear(titleInput);
    await userEvent.click(screen.getByRole("button", { name: /SAVE CHANGES/ }));
    expect(await screen.findByRole("alert")).toHaveTextContent(/All fields are required/);
    expect(api.put).not.toHaveBeenCalled();
  });

  it("rejects a non-positive price", async () => {
    renderWithRouter(<EditListing />);
    const priceInput = await screen.findByLabelText("Price (TimeCoin)");
    await userEvent.clear(priceInput);
    await userEvent.type(priceInput, "0");
    await userEvent.click(screen.getByRole("button", { name: /SAVE CHANGES/ }));
    expect(await screen.findByRole("alert")).toHaveTextContent(/greater than 0/);
    expect(api.put).not.toHaveBeenCalled();
  });
});

describe("EditListing — submit", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.setItem("token", "test-token");
    api.get.mockImplementation((url) => {
      if (url === "/listings/42") return Promise.resolve({ data: mockOwnedListing });
      if (url === "/wallet") return Promise.resolve({ data: mockWallet });
      return Promise.reject(new Error("unexpected URL"));
    });
  });

  it("sends a PUT with the edited fields and preserves status, then navigates to the detail page", async () => {
    api.put.mockResolvedValue({ data: { ...mockOwnedListing, title: "New Title" } });

    renderWithRouter(<EditListing />);
    const titleInput = await screen.findByLabelText("Title");
    await userEvent.clear(titleInput);
    await userEvent.type(titleInput, "New Title");
    await userEvent.click(screen.getByRole("button", { name: /SAVE CHANGES/ }));

    await waitFor(() => {
      expect(api.put).toHaveBeenCalledWith(
        "/listings/42",
        expect.objectContaining({
          title: "New Title",
          description: "Custom vector logo",
          price: 50,
          category: "Services",
          imageUrl: "https://example.com/logo.png",
          status: "ACTIVE",
        })
      );
    });

    await waitFor(() => {
      expect(mockedUsedNavigate).toHaveBeenCalledWith("/marketplace/42");
    });
  });

  it("surfaces the backend error message when PUT fails", async () => {
    api.put.mockRejectedValue({ response: { data: { message: "Server said no" } } });

    renderWithRouter(<EditListing />);
    await screen.findByLabelText("Title");
    await userEvent.click(screen.getByRole("button", { name: /SAVE CHANGES/ }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Server said no");
  });

  it("navigates back to the detail page when Cancel is clicked", async () => {
    renderWithRouter(<EditListing />);
    await screen.findByLabelText("Title");
    await userEvent.click(screen.getByRole("button", { name: /CANCEL/ }));
    expect(mockedUsedNavigate).toHaveBeenCalledWith("/marketplace/42");
  });
});
