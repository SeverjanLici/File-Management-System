import { WebStorageStateStore } from 'oidc-client-ts'

export const oidcConfig = {
  authority: import.meta.env.VITE_KEYCLOAK_AUTHORITY || 'http://localhost:8085/realms/docplatform',
  client_id: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'docplatform-frontend',
  redirect_uri: import.meta.env.VITE_REDIRECT_URI || 'http://localhost:5173/',
  post_logout_redirect_uri: import.meta.env.VITE_POST_LOGOUT_REDIRECT_URI || 'http://localhost:5173/',
  scope: 'openid profile email',
  response_type: 'code',
  userStore: new WebStorageStateStore({ store: window.localStorage }),
  onSigninCallback: () => {
    window.history.replaceState({}, document.title, window.location.pathname)
  },
}
