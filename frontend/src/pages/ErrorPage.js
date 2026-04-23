import React from 'react';
import { useNavigate } from 'react-router-dom';
import '../ErrorPage.css';

const ERROR_MESSAGES = {
    400: { title: 'Bad Request', detail: 'The server could not understand the request.' },
    403: { title: 'Forbidden', detail: 'You do not have permission to access this resource.' },
    404: { title: 'Not Found', detail: 'The page or resource you are looking for does not exist.' },
    429: { title: 'Too Many Requests', detail: 'You have sent too many requests. Please wait before trying again.' },
    500: { title: 'Server Error', detail: 'Something went wrong on our end. Please try again later.' },
    502: { title: 'Bad Gateway', detail: 'The server received an invalid response from an upstream server.' },
    503: { title: 'Service Unavailable', detail: 'The service is temporarily unavailable. Please try again later.' },
};

function ErrorPage() {
    const navigate = useNavigate();

    const params = new URLSearchParams(window.location.search);
    const status = parseInt(params.get('status'), 10) || 0;
    const customMessage = params.get('message') || '';
    const retryAfter = params.get('retryAfter') || '';

    const known = ERROR_MESSAGES[status];
    const title = known ? known.title : 'Unexpected Error';
    const detail = customMessage || (known ? known.detail : 'An unexpected error occurred. Please try again.');

    return (
        <div className="error-page-wrapper">
            <div className="error-page-card">
                {status > 0 && (
                    <div className="error-status-code">{status}</div>
                )}

                <h1 className="error-title">{title}</h1>
                <p className="error-detail">{detail}</p>

                {status === 429 && retryAfter && (
                    <p className="error-retry-after">
                        You can try again in <strong>{retryAfter}</strong>.
                    </p>
                )}

                <div className="error-actions">
                    <button
                        className="error-btn error-btn-primary"
                        onClick={() => navigate('/dashboard')}
                    >
                        Go Home
                    </button>
                    <button
                        className="error-btn error-btn-secondary"
                        onClick={() => navigate(-1)}
                    >
                        Go Back
                    </button>
                </div>
            </div>
        </div>
    );
}

export default ErrorPage;
