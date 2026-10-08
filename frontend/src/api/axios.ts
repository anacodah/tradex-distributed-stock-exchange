import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api', // Assuming gateway runs on 8080 locally or via docker-compose mapping
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;
