// This file will be loaded in index.html
// Placeholders are replaced by entrypoint.sh at container start.
// NON-SECRET values only: booleans, provider ids and the API base URL.
// Never put an issuer URL, client id or client secret here.
window.runtimeConfig = {
  API_BASE_URL: "##VITE_API_BASE_URL##",
  SOCIAL_LOGIN_ENABLED: "##VITE_SOCIAL_LOGIN_ENABLED##",
  SOCIAL_PROVIDERS: "##VITE_SOCIAL_PROVIDERS##"
};
