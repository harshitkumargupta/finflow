import axios from "./axios";

export const getCards = () => axios.get("/api/cards");
export const getCard = (id) => axios.get(`/api/cards/${id}`);
export const addCard = (data) => axios.post("/api/cards", data);
export const toggleCardStatus = (id) => axios.put(`/api/cards/${id}/status`);
export const deleteCard = (id) => axios.delete(`/api/cards/${id}`);
