A company requires a system to efficiently manage internal documents and support
collaboration among employees. The system must allow secure uploading,
organization, processing, and sharing of documents within the organization.
The system is used by several kinds of users:

- Admins: Global system control, user and category management, full access to
  all documents, and monitoring of system behavior.
- Managers: Operate within a department, managing departmental documents
  and spaces.
- Staff: Create and manage personal documents and access departmental or
  explicitly shared documents.

Through a user-friendly front-end, users of the system should have access to the
following functionalities:

1. Document upload
   a. Upload documents using a resumable upload mechanism
   b. Supported file types include at least PDF, DOCX, video and image files
2. Metadata management
   a. Associate metadata with documents, such as title, category, tags,
   description, and summary
   b. Manage multiple immutable versions of the same document
3. Document access and sharing
   a. View, download, and manage documents according to roles and
   permissions
   b. Share documents with other users, with explicit access rules
   c. Enforce access control based on roles, ownership, department, and
   sharing relationships
4. Search and retrieval
   a. Search and filter documents based on metadata and access
   permissions
   b. Preview documents when feasible or download them for local use
5. Automated document processing
   a. Execute background processing on documents after upload, such as
   automatic metadata generation or classification
   b. Background processing must not block user-facing operations
   c. The system must expose the status and outcome of such processing
6. Monitoring and evaluation
   a. Monitor the execution of background and AI-based processing
   pipelines
   i.
   Track processing status, failures, retries, and execution times
   b. Provide traceability between documents, processing events, and
   generated outputs
   c. Support evaluation and validation of automatically generated
   metadata
   e. Allow manual intervention on automated processing when required
   (e.g. re-execution or override)
