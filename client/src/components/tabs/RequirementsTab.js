export default function RequirementsTab({ requirements }) {
  if (!requirements) return null;

  return (
    <section className="panel-grid">
      <article className="panel wide">
        <h2>Prioritized User Stories</h2>
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Priority</th>
              <th>User Story</th>
            </tr>
          </thead>
          <tbody>
            {requirements.userStories.map((s) => (
              <tr key={s.id}>
                <td>{s.id}</td>
                <td>{s.priority}</td>
                <td>{s.story}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </article>

      <article className="panel wide">
        <h2>Conflicting Requirements and Trade-Offs</h2>
        <table>
          <thead>
            <tr>
              <th>Conflict</th>
              <th>Trade-Off</th>
            </tr>
          </thead>
          <tbody>
            {requirements.conflictsAndTradeOffs.map((item, idx) => (
              <tr key={idx}>
                <td>{item.conflict}</td>
                <td>{item.tradeOff}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </article>

      <article className="panel">
        <h2>High Priority Prototypes</h2>
        <ul className="feed">
          {requirements.highPriorityPrototypes.map((p) => (
            <li key={p}>{p}</li>
          ))}
        </ul>
      </article>
    </section>
  );
}
