import { defineStore } from 'pinia';

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null,
    token: ''
  }),
  actions: {
    setUser(info) {
      this.userInfo = info;
    },
    setToken(token) {
      this.token = token;
    }
  }
});
