import { Outlet } from "react-router-dom";
import { SidebarProvider } from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/app-sidebar";
import { TooltipProvider } from "@/components/ui/tooltip";

function AppLayout() {
  return (
    <SidebarProvider>
      <TooltipProvider>
        <AppSidebar />
        <main className="flex-1 p-4">
          <Outlet />
        </main>
      </TooltipProvider>
    </SidebarProvider>
  );
}

export default AppLayout;
