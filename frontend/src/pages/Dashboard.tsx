import { Link } from "react-router-dom";
import { useMyDocuments } from "../hooks/useDocuments";
import { useCurrentUser } from "../hooks/useUser";
import DocumentCard from "../components/DocumentCard";
import Loading from "../components/Loading";
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";

function Dashboard() {
  const { data: user, isLoading: userLoading } = useCurrentUser();
  const { data: documents, isLoading: docsLoading } = useMyDocuments(0, 6);

  if (userLoading) return <Loading />;

  return (
    <div className="flex flex-col gap-8">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">
          Welcome, {user?.firstName}!
        </h1>
        <p className="mt-1 text-gray-600">
          Manage your documents and collaborate with your team.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Link to="/upload" className="block">
          <Card className="bg-blue-600 text-white hover:bg-blue-700 transition-colors">
            <CardHeader>
              <CardTitle className="text-lg font-semibold">
                Upload Document
              </CardTitle>
              <CardDescription className="mt-2 text-blue-100">
                Upload and share new documents with your team
              </CardDescription>
            </CardHeader>
            <CardContent />
          </Card>
        </Link>

        <Link to="/documents" className="block">
          <Card className="bg-white rounded-lg">
            <CardHeader>
              <CardTitle className="text-lg font-semibold text-gray-900">
                Browse Documents
              </CardTitle>
              <CardDescription className="mt-2 text-gray-600">
                View all documents shared with you
              </CardDescription>
            </CardHeader>
            <CardContent />
          </Card>
        </Link>

        <Link to="/departments" className="block">
          <Card className="bg-white rounded-lg">
            <CardHeader>
              <CardTitle className="text-lg font-semibold text-gray-900">
                Departments
              </CardTitle>
              <CardDescription className="mt-2 text-gray-600">
                Manage departments and members
              </CardDescription>
            </CardHeader>
            <CardContent />
          </Card>
        </Link>
      </div>

      <div className="flex flex-col gap-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-gray-900">
            My Recent Documents
          </h2>
          <Link to="/documents">
            <Button variant="link" size="sm">
              View all
            </Button>
          </Link>
        </div>

        {docsLoading ? (
          <div className="text-center py-8 text-gray-500">
            Loading documents...
          </div>
        ) : documents?.content.length === 0 ? (
          <div className="text-center py-8 bg-white rounded-lg shadow">
            <p className="text-gray-500">No documents yet</p>
            <Link
              to="/upload"
              className="text-blue-600 hover:text-blue-700 mt-2 inline-block"
            >
              Upload your first document
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {documents?.content.map((doc) => (
              <DocumentCard key={doc.id} document={doc} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default Dashboard;
