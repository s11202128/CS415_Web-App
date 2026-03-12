export default function SiteFooter({ currentYear }) {
  return (
    <footer className="site-footer">
      <p>Bank of Fiji Online Banking</p>
      <p>Support: support@bof.fj | Hotline: +679 7899369</p>
      <p>Copyright {currentYear} Bank of Fiji. All rights reserved.</p>
    </footer>
  );
}
