import { useState, useEffect, useRef } from "react";
import { useAiChat } from "../hooks/useAiChat";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Loader2, Send, MessageCircle } from "lucide-react";

export function AiChatPanel() {
  const [question, setQuestion] = useState("");
  const { messages, loading, error, askGlobal } = useAiChat();
  const scrollRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom when new messages arrive
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages, loading]);

  const handleAsk = async () => {
    if (!question.trim() || loading) return;

    try {
      await askGlobal(question);
      setQuestion("");
    } catch (err) {
      console.error("Error asking question:", err);
    }
  };

  return (
    <div className="flex flex-col h-full p-4 gap-4">
      {/* Header */}
      <div className="border-b pb-3">
        <h2 className="text-lg font-semibold text-gray-900 flex items-center gap-2">
          <MessageCircle className="w-5 h-5" />
          Ask AI
        </h2>
        <p className="text-xs text-gray-500 mt-1">
          Ask questions about your documents
        </p>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-hidden">
        <ScrollArea className="h-full pr-4">
          <div
            ref={scrollRef}
            className="space-y-3 pb-4"
          >
            {messages.length === 0 ? (
              <div className="text-center text-gray-400 py-8">
                <MessageCircle className="w-8 h-8 mx-auto opacity-50 mb-2" />
                <p className="text-sm">
                  Start asking questions about your documents
                </p>
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
                    className={`max-w-[75%] px-3 py-2 rounded-lg text-sm ${
                      msg.role === "user"
                        ? "bg-blue-600 text-white"
                        : "bg-gray-200 text-gray-900"
                    }`}
                  >
                    <p className="whitespace-pre-wrap break-words">
                      {msg.content}
                    </p>
                  </div>
                </div>
              ))
            )}
            {error && (
              <div className="text-xs text-red-600 bg-red-50 p-2 rounded">
                Error: {error}
              </div>
            )}
            {loading && (
              <div className="flex items-center gap-2 text-gray-500">
                <Loader2 className="w-4 h-4 animate-spin" />
                <span className="text-xs">Thinking...</span>
              </div>
            )}
          </div>
        </ScrollArea>
      </div>

      {/* Input */}
      <div className="border-t pt-3 flex gap-2">
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
          className="text-sm"
        />
        <Button
          onClick={handleAsk}
          disabled={loading || !question.trim()}
          size="sm"
          variant="default"
        >
          {loading ? (
            <Loader2 className="w-4 h-4 animate-spin" />
          ) : (
            <Send className="w-4 h-4" />
          )}
        </Button>
      </div>
    </div>
  );
}


