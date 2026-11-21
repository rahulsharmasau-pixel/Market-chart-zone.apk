import { useState, useEffect } from "react";
import { AppShell } from "@/components/AppShell";
import { HomeScreen } from "@/components/screens/HomeScreen";
import { TradesScreen } from "@/components/screens/TradesScreen";
import { MarketScreen } from "@/components/screens/MarketScreen";
import { CommunityScreen } from "@/components/screens/CommunityScreen";
import { ToolsScreen } from "@/components/screens/ToolsScreen";
import { AccountScreen } from "@/components/screens/AccountScreen";
import { AdminGate } from "@/components/screens/AdminGate";
import { AuthScreen } from "@/components/screens/AuthScreen";
import { StoreProvider, useStore } from "@/lib/store";

export type Tab = "home" | "trades" | "market" | "community" | "tools";

function Shell() {
  const { user, theme } = useStore();
  const [tab, setTab] = useState<Tab>("home");
  const [overlay, setOverlay] = useState<"none" | "account" | "admin">("none");

  useEffect(() => {
    document.documentElement.classList.toggle("dark", theme === "dark");
  }, [theme]);

  if (!user) {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center p-4">
        <AuthScreen />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-950 flex items-center justify-center p-0 sm:p-6">
      <div className="relative w-full max-w-md h-screen sm:h-[860px] sm:rounded-3xl overflow-hidden shadow-2xl ring-1 ring-white/10">
        <AppShell
          tab={tab}
          setTab={setTab}
          onOpenAccount={() => setOverlay("account")}
        >
          {tab === "home" && (
            <HomeScreen onOpenAccount={() => setOverlay("account")} />
          )}
          {tab === "trades" && <TradesScreen />}
          {tab === "market" && <MarketScreen />}
          {tab === "community" && <CommunityScreen />}
          {tab === "tools" && <ToolsScreen />}
        </AppShell>

        {overlay === "account" && (
          <AccountScreen
            onClose={() => setOverlay("none")}
            onOpenAdmin={() => setOverlay("admin")}
          />
        )}
        {overlay === "admin" && <AdminGate onClose={() => setOverlay("none")} />}
      </div>
    </div>
  );
}

export default function App() {
  return (
    <StoreProvider>
      <Shell />
    </StoreProvider>
  );
}
