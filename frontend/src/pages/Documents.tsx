import { useState } from "react";
import { Link } from "react-router-dom";
import { useDocuments } from "../hooks/useDocuments";
import DocumentCard from "../components/DocumentCard";
import Loading from "../components/Loading";
import { Button } from "@/components/ui/button";
import { AiChatPanel } from "../components/AiChatPanel";

function Documents() {
  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState("");
  const [categoryInput, setCategoryInput] = useState("");
  const [tagInput, setTagInput] = useState("");
  const [filters, setFilters] = useState({ search: "", category: "", tag: "" });
  const { data, isLoading, error } = useDocuments(page, 12, filters);

  if (isLoading) return <Loading />;

  if (error) {
    return (
      <div className="text-center py-8">
        <p className="text-red-600">Error loading documents</p>
      </div>
    );
  }

  return (
    <div className="flex gap-8 h-[calc(100vh-120px)]">
      {/* Left: Documents list */}
      <div className="flex-1 overflow-y-auto flex flex-col gap-8 pb-8">
        <div className="flex justify-between items-center sticky top-0 bg-white z-10 pb-4">
          <h1 className="text-2xl font-bold text-gray-900">All Documents</h1>
          <Link to="/upload">
            <Button variant="default">Upload New</Button>
          </Link>
        </div>

      <form
        className="grid grid-cols-1 md:grid-cols-4 gap-3"
        onSubmit={(event) => {
          event.preventDefault();
          setFilters({
            search: searchInput,
            category: categoryInput,
            tag: tagInput,
          });
          setPage(0);
        }}
      >
        <input
          value={searchInput}
          onChange={(event) => setSearchInput(event.target.value)}
          placeholder="Search name, description, summary, tags..."
          className="md:col-span-2 rounded-md border px-3 py-2 text-sm"
        />
        <input
          value={categoryInput}
          onChange={(event) => setCategoryInput(event.target.value)}
          placeholder="Filter by category"
          className="rounded-md border px-3 py-2 text-sm"
        />
        <div className="flex gap-2">
          <input
            value={tagInput}
            onChange={(event) => setTagInput(event.target.value)}
            placeholder="Filter by tag"
            className="flex-1 rounded-md border px-3 py-2 text-sm"
          />
          <Button type="submit" variant="secondary">
            Apply
          </Button>
        </div>
      </form>

      {data?.content.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-lg shadow">
          <p className="text-gray-500 mb-4">No documents found</p>
          <Link to="/upload" className="text-blue-600 hover:text-blue-700">
            Upload your first document
          </Link>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {data?.content.map((doc) => (
              <DocumentCard key={doc.id} document={doc} />
            ))}
          </div>

          {data && data.totalPages > 1 && (
            <div className="mt-6 flex justify-center space-x-2">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={!data.hasPrevious}
                className="px-4 py-2 border rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
              >
                Previous
              </button>
              <span className="px-4 py-2 text-gray-600">
                Page {data.page + 1} of {data.totalPages}
              </span>
              <button
                onClick={() => setPage((p) => p + 1)}
                disabled={!data.hasNext}
                className="px-4 py-2 border rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
              >
                Next
              </button>
            </div>
           )}
         </>
       )}
      </div>

      {/* Right: Chat Panel */}
      <div className="w-96 border-l bg-white flex flex-col">
        <AiChatPanel />
      </div>
    </div>
  );
}

export default Documents;
