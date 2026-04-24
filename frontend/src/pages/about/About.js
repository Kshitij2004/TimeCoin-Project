import { Link } from 'react-router-dom';
import './About.css';

const TEAM_MEMBERS = [
  {
    name:  'Kshitij Pandey',
    role:  'Scrum Master 3 - Backend Developer',
    email: 'kpandey4@wisc.edu',
    photo: null,
  },
  {
    name:  'Tianze Gao',
    role:  'Product Owner 3 - Frontend Developer',
    email: 'tgao67@wisc.edu',
    photo: null,
  },
  {
    name:  'Jeremiah Jin',
    role:  'Scrum Master 2 - Backend Developer',
    email: 'zjin254@wisc.edu',
    photo: null,
  },
  {
    name:  'Dominick Weston',
    role:  'Product Owner 2 - Backend Developer',
    email: 'dweston@wisc.edu',
    photo: null,
  },
  {
    name:  'Andrew McDonagh',
    role:  'Scrum Master 0/1 - Backend Developer',
    email: 'almdconagh@wisc.edu',
    photo: null,
  },
  {
    name:  'Garv Pundir',
    role:  'Product Owner 0/1 - Frontend Developer',
    email: 'gpundir@wisc.edu',
    photo: null,
  },
];

const HOW_IT_WORKS = [
  {
    title: 'Blocks',
    body:
      'Every transaction on TimeCoin is grouped into a block. Each block contains a cryptographic hash of the previous block, chaining them together in a tamper-evident sequence — the blockchain.',
  },
  {
    title: 'Transactions',
    body:
      'When you send TimeCoin (TC) to another user, a transaction is broadcast to the network. It sits in a pending pool until a new block is committed, at which point it becomes confirmed and irreversible.',
  },
  {
    title: 'Staking',
    body:
      'Holding TC earns you staking rewards over time. Staked coins are locked and contribute to network security. Your dashboard shows your available balance separately from your staked amount.',
  },
];

function InitialsAvatar({ name }) {
  const initials = name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
  return <div className="about-avatar" aria-hidden="true">{initials}</div>;
}

export default function About() {
  return (
    <div className="about-page">
      <aside className="about-sidebar">
        <div className="about-logo">CrypMart</div>
        <nav className="about-nav">
          <Link to="/login"       className="about-nav-link">Log In</Link>
          <Link to="/about"       className="about-nav-link about-nav-link--active">About</Link>
        </nav>
      </aside>

      <main className="about-main">

        {/* ── Hero ── */}
        <section className="about-hero">
          <div className="about-hero-eyebrow">About</div>
          <h1 className="about-hero-title">TimeCoin</h1>
          <p className="about-hero-sub">
            A campus-scale blockchain economy built for the gig generation.
            Earn, trade, and stake TC — a digital currency backed by real
            student services on the UW–Madison campus.
          </p>
          <a
            href="https://git.doit.wisc.edu/cdis/cs/courses/cs506/sp2026/team/t_12/Project_12/-/blob/main/README.md?ref_type=heads"
            className="about-repo-link"
            target="_blank"
            rel="noopener noreferrer"
          >
            View on GitLab →
          </a>
        </section>

        {/* ── What is TimeCoin ── */}
        <section className="about-section">
          <h2 className="about-section-title">What is TimeCoin?</h2>
          <p className="about-body">
            TimeCoin (TC) is a peer-to-peer digital currency for the campus
            community. Students can list services on the marketplace — tutoring,
            design work, deliveries — and get paid directly in TC. Every
            transaction is recorded on a transparent, append-only blockchain,
            giving both buyers and sellers a permanent, verifiable record.
          </p>
          <p className="about-body">
            The platform is built with a Spring Boot backend, a MySQL ledger,
            and a React frontend. The Blockchain Explorer lets you watch the
            chain grow in real time and drill into any block or transaction.
          </p>
        </section>

        {/* ── How the blockchain works ── */}
        <section className="about-section">
          <h2 className="about-section-title">How the Blockchain Works</h2>
          <div className="about-how-grid">
            {HOW_IT_WORKS.map((item, i) => (
              <div key={item.title} className="about-how-card">
                <div className="about-how-number">0{i + 1}</div>
                <h3 className="about-how-title">{item.title}</h3>
                <p className="about-how-body">{item.body}</p>
              </div>
            ))}
          </div>
        </section>

        {/* ── Team ── */}
        <section className="about-section">
          <h2 className="about-section-title">Team</h2>
          <div className="about-team-grid">
            {TEAM_MEMBERS.map((member) => (
              <div key={member.name} className="about-team-card">
                {member.photo
                  ? <img src={member.photo} alt={member.name} className="about-avatar about-avatar--photo" />
                  : <InitialsAvatar name={member.name} />
                }
                <div className="about-team-info">
                  <div className="about-team-name">{member.name}</div>
                  <div className="about-team-role">{member.role}</div>
                  <a href={`mailto:${member.email}`} className="about-team-email">{member.email}</a>
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* ── Repo link ── */}
        <section className="about-section about-section--last">
          <h2 className="about-section-title">Project Repository</h2>
          <p className="about-body">
            The full source code, issue tracker, and CI/CD pipeline live on
            GitLab. Feel free to browse the codebase, open issues, or fork the
            project.
          </p>
          <a
            href="https://git.doit.wisc.edu/cdis/cs/courses/cs506/sp2026/team/t_12/Project_12"
            className="about-repo-btn"
            target="_blank"
            rel="noopener noreferrer"
          >
            View Repository →
          </a>
        </section>

      </main>
    </div>
  );
}