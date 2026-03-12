export default function SiteFooter({ currentYear }) {
  return (
    <footer className="site-footer">
      <p>Bank of Fiji Online Banking Prototype</p>
      <p>Support: support@bof.fj | Hotline: +679 132</p>
      <p>Copyright {currentYear} Bank of Fiji. All rights reserved.</p>
    </footer>
  );
}
