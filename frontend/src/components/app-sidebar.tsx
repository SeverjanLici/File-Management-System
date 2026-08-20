"use client";

import * as React from "react";

import { NavMain } from "@/components/nav-main";
import { NavUser } from "@/components/nav-user";
import { useAuth } from "react-oidc-context";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from "@/components/ui/sidebar";
import {
    BookIcon,
    BuildingIcon,
    FileIcon,
    LayoutDashboardIcon,
    UploadIcon,
    CpuIcon,
} from "lucide-react";

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
    const auth = useAuth();

    const profile = auth.user?.profile as any;

    const roles: string[] =
        profile?.realm_access?.roles || [];

    const isAdmin = roles.includes("ADMIN");

  const navItems = [
    { url: "/", title: "Dashboard", icon: <LayoutDashboardIcon /> },
    { url: "/documents", title: "Documents", icon: <FileIcon /> },
    { url: "/upload", title: "Upload", icon: <UploadIcon /> },
    { url: "/departments", title: "Departments", icon: <BuildingIcon /> },
    //{ url: "/processing", title: "Processing", icon: <CpuIcon /> },
      ...(isAdmin
          ? [{ url: "/processing", title: "Processing", icon: <CpuIcon /> }]
          : []),
    ];


  return (
    <Sidebar {...props}>
      <SidebarHeader>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton size="lg" asChild>
              <a href="#">
                <div className="flex aspect-square size-8 items-center justify-center rounded-lg bg-sidebar-primary text-sidebar-primary-foreground">
                  <BookIcon className="size-4" />
                </div>
                <div className="grid flex-1 text-left text-sm leading-tight">
                  <span className="truncate font-medium">DocPlatform</span>
                </div>
              </a>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>
      <SidebarContent>
        <NavMain items={navItems} />
      </SidebarContent>
      <SidebarFooter>
        <NavUser />
      </SidebarFooter>
    </Sidebar>
  );
}
