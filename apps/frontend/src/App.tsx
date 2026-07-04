import { useEffect, useState } from "react";
import { API_URL } from "./env";

interface Item {
  id: number;
  name: string;
  category: string;
}

type State =
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "ready"; items: Item[] };

export default function App() {
  const [state, setState] = useState<State>({ status: "loading" });

  useEffect(() => {
    fetch(`${API_URL}/api/items`)
      .then((res) => {
        if (!res.ok) throw new Error(`API responded ${res.status}`);
        return res.json() as Promise<Item[]>;
      })
      .then((items) => setState({ status: "ready", items }))
      .catch((err: Error) => setState({ status: "error", message: err.message }));
  }, []);

  return (
    <main style={{ fontFamily: "system-ui, sans-serif", maxWidth: 640, margin: "3rem auto" }}>
      <h1>Items Dashboard</h1>
      <p style={{ color: "#666" }}>
        Stub page proving frontend → backend → database connectivity. API:{" "}
        <code>{API_URL || "same-origin"}</code>
      </p>

      {state.status === "loading" && <p>Loading…</p>}
      {state.status === "error" && (
        <p style={{ color: "crimson" }}>Failed to load items: {state.message}</p>
      )}
      {state.status === "ready" && (
        <table style={{ borderCollapse: "collapse", width: "100%" }} border={1}>
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Category</th>
            </tr>
          </thead>
          <tbody>
            {state.items.map((item) => (
              <tr key={item.id}>
                <td>{item.id}</td>
                <td>{item.name}</td>
                <td>{item.category}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </main>
  );
}
