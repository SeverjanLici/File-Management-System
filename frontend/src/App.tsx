import { Routes, Route, Navigate } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import AppLayout from "./layouts/AppLayout";
import Dashboard from "./pages/Dashboard";
import Documents from "./pages/Documents";
import DocumentDetail from "./pages/DocumentDetail";
import Upload from "./pages/Upload";
import Departments from "./pages/Departments";
import DepartmentDetail from "./pages/DepartmentDetail";
import LoginCallback from "./pages/LoginCallback";
import Loading from "./components/Loading";
import SignInCard from "./components/SignInCard";
import ProcessingDashboard from "./pages/ProcessingDashboard.tsx";

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const auth = useAuth();

  if (auth.isLoading) {
    return <Loading />;
  }

  if (!auth.isAuthenticated) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <SignInCard auth={auth} />
      </div>
    );
  }

  return <>{children}</>;
}

function RoleProtectedRoute({
                                children,
                                requiredRole,
                            }: {
    children: React.ReactNode;
    requiredRole: string;
}) {
    const auth = useAuth();

    if (auth.isLoading) return <Loading />;

    if (!auth.isAuthenticated) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <SignInCard auth={auth} />
            </div>
        );
    }

    const roles =
        (auth.user?.profile as any)?.realm_access?.roles || [];
    //console.log(auth.user?.profile);
    console.log(roles);
    if (!roles.includes(requiredRole)) {
        return (
            <div className="p-6 text-red-600">
                Access denied (missing role: {requiredRole})
            </div>
        );
    }

    return <>{children}</>;
}

function App() {
  const auth = useAuth();

  if (auth.error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100">
        <div className="bg-white p-8 rounded-lg shadow-md">
          <h1 className="text-xl font-bold text-red-600 mb-4">
            Authentication Error
          </h1>
          <p className="text-gray-600">{auth.error.message}</p>
          <button
            onClick={() => auth.signinRedirect()}
            className="mt-4 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
          >
            Try Again
          </button>
        </div>
      </div>
    );
  }

  return (
    <Routes>
      <Route path="/callback" element={<LoginCallback />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Dashboard />} />
        <Route path="documents" element={<Documents />} />
        <Route path="documents/:id" element={<DocumentDetail />} />
        <Route path="upload" element={<Upload />} />
        <Route path="departments" element={<Departments />} />
        <Route path="departments/:id" element={<DepartmentDetail />} />
          <Route
              path="processing"
              element={
                  <RoleProtectedRoute requiredRole="ADMIN">
                      <ProcessingDashboard />
                  </RoleProtectedRoute>
              }
          />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
