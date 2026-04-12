## Authentication

### Login
**POST** `/api/auth/login`

Authenticates a user and returns two tokens.

**Request body:**
```json
{
    "username": "your_username",
    "password": "your_password"
}
```

**Response:**
```json
{
    "accessToken": "eyJ...",
    "refreshToken": "550e8400-e29b-41d4-a716..."
}
```

---

### Token Usage
Include the access token in the `Authorization` header for all protected endpoints:

```json
Authorization: Bearer <accessToken>
```

Access tokens expire after **1 hour**. When a request returns `401` with the message `"Token has expired"`, use the refresh endpoint to get a new one.

---

### Refresh
**POST** `/api/auth/refresh`

Exchanges a valid refresh token for a new access token.

**Request body:**
```json
{
    "refreshToken": "550e8400-e29b-41d4-a716..."
}
```

**Response:**
```json
{
    "accessToken": "eyJ..."
}
```

Refresh tokens expire after **7 days**. If the refresh token is expired or invalid, the user must log in again.

---

### Error Responses

| Status | Message | Meaning |
|--------|---------|---------|
| 401 | `"Invalid credentials"` | Wrong username or password |
| 401 | `"Token has expired"` | Access token is expired — refresh it |
| 401 | `"Invalid refresh token"` | Refresh token not found — log in again |
| 401 | `"Refresh token has expired"` | Refresh token expired — log in again |