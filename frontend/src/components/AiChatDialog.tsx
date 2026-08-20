import { useState } from "react";
import { useAiChat } from "../hooks/useAiChat";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Loader2, Send, MessageCircle } from "lucide-react";

interface AiChatDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  documentId?: string;
  mode: "file" | "global";
}

export function AiChatDialog({
  open,
  onOpenChange,
  documentId,
  mode,
}: AiChatDialogProps) {
  const [question, setQuestion] = useState("");
  // Use a specific key for document-specific chats so they don't overwrite each other
  const chatKey = mode === "file" && documentId ? `file-${documentId}` : "global";
  const { messages, loading, error, askAboutFile, askGlobal, clearMessages } =
    useAiChat(chatKey);

  const handleAsk = async () => {
    if (!question.trim()) return;

    try {
      if (mode === "file" && documentId) {
        await askAboutFile(documentId, question);
      } else {
        await askGlobal(question);
      }
      setQuestion("");
    } catch (err) {
      console.error("Error asking question:", err);
    }
  };

  const handleOpenChange = (newOpen: boolean) => {
    onOpenChange(newOpen);
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-md md:max-w-lg h-[600px] flex flex-col">
        <DialogHeader>
          <DialogTitle className="flex items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <MessageCircle className="w-5 h-5" />
              {mode === "file" ? "Ask About Document" : "Ask About All Documents"}
            </div>
            {messages.length > 0 && (
              <Button variant="ghost" size="sm" onClick={clearMessages} className="text-xs text-red-500 hover:text-red-700">
                Clear Chat
              </Button>
            )}
          </DialogTitle>
          <DialogDescription>
            {mode === "file"
              ? "Ask questions about this document"
              : "Ask questions across all your documents"}
          </DialogDescription>
        </DialogHeader>

        <ScrollArea className="flex-1 pr-4 mb-4">
          <div className="space-y-4">
            {messages.length === 0 ? (
              <div className="text-center text-gray-400 py-8">
                <p>No messages yet. Ask a question to get started!</p>
              </div>
            ) : (
              messages.map((msg) => (
                <div
                  key={msg.id}
                  className={`flex ${
                    msg.role === "user" ? "justify-end" : "justify-start"
                  }`}
                >
                  <div
                    className={`max-w-[85%] px-4 py-2 rounded-lg ${
                      msg.role === "user"
                        ? "bg-blue-600 text-white"
                        : "bg-gray-200 text-gray-900"
                    }`}
                  >
                    <p className="text-sm whitespace-pre-wrap">{msg.content}</p>
                  </div>
                </div>
              ))
            )}
            {error && (
              <div className="text-sm text-red-600 bg-red-50 p-3 rounded">
                Error: {error}
              </div>
            )}
            {loading && (
              <div className="flex items-center gap-2 text-gray-500">
                <Loader2 className="w-4 h-4 animate-spin" />
                <span className="text-sm">Thinking...</span>
              </div>
            )}
          </div>
        </ScrollArea>

        <div className="flex gap-2 mt-auto">
          <Input
            placeholder="Ask a question..."
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            onKeyPress={(e) => {
              if (e.key === "Enter" && !loading) {
                handleAsk();
              }
            }}
            disabled={loading}
          />
          <Button
            onClick={handleAsk}
            disabled={loading || !question.trim()}
            size="sm"
          >
            {loading ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Send className="w-4 h-4" />
            )}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
