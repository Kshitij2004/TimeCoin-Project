import api from './api.js'; // Use project's custom api instance

export const mineCoin = async () => {
    // Backend path: POST /api/mining/mine
    return await api.post('/mining/mine');
};

export const fetchMiningStats = async (walletAddress) => {
    // Backend path: GET /api/mining/stats/{walletAddress}
    return await api.get(`/mining/stats/${walletAddress}`);
};