import React, { useEffect, useState } from 'react'
import { createRoot } from 'react-dom/client'
import {
  Activity, AlertTriangle, ArrowUpRight, Bell, Check, ChevronDown, ChevronRight,
  CircleHelp, Database, Gauge, KeyRound, Layers3, LogOut, Menu, Play, Plus,
  RefreshCw, Search, Settings2, ShieldCheck, SlidersHorizontal, Table2, X, Zap
} from 'lucide-react'
import './styles.css'
import './visual-overrides.css'

const demoMode = import.meta.env.VITE_DEMO_MODE !== 'false'
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const demo = {
  tables: [{ name: 'clients', catalog: 'bd_bank' }, { name: 'comptes', catalog: 'bd_bank' }, { name: 'transactions', catalog: 'bd_bank' }, { name: 'agences', catalog: 'bd_bank' }],
  scope: ['clients', 'comptes', 'transactions'],
  rules: [
    { id: 1, dslText: 'solde < 0 AND decouvert_autorise = false', targetTable: 'comptes', severity: 'CRITICAL', active: true, createdAt: '2026-08-31T09:18:00' },
    { id: 2, dslText: 'montant > 10000', targetTable: 'transactions', severity: 'HIGH', active: true, createdAt: '2026-08-30T14:42:00' },
    { id: 3, dslText: 'email IS NULL', targetTable: 'clients', severity: 'MEDIUM', active: true, createdAt: '2026-08-29T11:06:00' },
    { id: 4, dslText: 'COUNT(transactions) > 200', targetTable: 'comptes', severity: 'LOW', active: false, createdAt: '2026-08-28T08:55:00' }
  ],
  alerts: [
    { id: 1048, ruleId: 1, status: 'ACTIVE', detectedAt: '2026-09-02T09:41:00', violatingEntityId: 'CPT-00842', involvedColumns: ['solde', 'decouvert_autorise'], consecutiveDetections: 4 },
    { id: 1047, ruleId: 2, status: 'ACTIVE', detectedAt: '2026-09-02T09:38:00', violatingEntityId: 'TX-44921', involvedColumns: ['montant'], consecutiveDetections: 2 },
    { id: 1046, ruleId: 3, status: 'ACTIVE', detectedAt: '2026-09-02T09:35:00', violatingEntityId: 'CLI-00213', involvedColumns: ['email'], consecutiveDetections: 1 },
    { id: 1045, ruleId: 2, status: 'RESOLVED', detectedAt: '2026-09-01T16:22:00', resolvedAt: '2026-09-02T08:12:00', violatingEntityId: 'TX-44780', involvedColumns: ['montant'], consecutiveDetections: 3 }
  ],
  frequency: { interval: '5m', cronExpression: null, enabled: 'true' }
}

const labels = { CRITICAL: 'Critique', HIGH: 'Haute', MEDIUM: 'Moyenne', LOW: 'Faible' }
const demoUser = { username: 'admin', role: 'ADMIN' }
const nav = [
  { id: 'overview', label: 'Vue d’ensemble', icon: Gauge },&
  { id: 'schema', label: 'Schéma & périmètre', icon: Database },
  { id: 'settings', label: 'Configuration', icon: Settings2 }
]

function formatDate(value) {
  return new Intl.DateTimeFormat('fr-FR', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}

function formatTime(value) {
  return new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}

async function request(path, options = {}) {
  const token = sessionStorage.getItem('bridge-token')
  let response
  try {
    response = await fetch(`${API_URL}${path}`, { ...options, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers } })
  } catch (error) {
    throw new Error(`Backend inaccessible (${API_URL})`)
  }
  if (response.status === 401) throw new Error('SESSION_EXPIRED')
  if (!response.ok) { const body = await response.json().catch(() => ({})); const error = new Error(body.message || body.error || 'Une erreur est survenue'); error.status = response.status; throw error }
  return response.status === 204 ? null : response.json()
}

async function login(username, password) {
  const response = await fetch(`${API_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  })
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new Error(body.message || body.error || 'Identifiants invalides')
  }
  return response.json()
}

async function setup(username, password, setupKey) {
  const response = await fetch(`${API_URL}/auth/setup`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Setup-Key': setupKey },
    body: JSON.stringify({ username, password })
  })
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new Error(body.message || body.error || 'Initialisation refusée')
  }
}

function useWorkspace(enabled) {
  const [data, setData] = useState(demo)
  const [busy, setBusy] = useState(false)
  const loadWorkspace = () => Promise.all([
    request('/schema/tables'),
    request('/scope'),
    request('/rules?page=0&size=25'),
    request('/alerts?page=0&size=25'),
    request('/config/frequency').catch((error) => error.status === 404 ? null : Promise.reject(error))
  ]).then(([tables, scope, rules, alerts, frequency]) => setData({ tables, scope: scope.map((item) => item.name), rules: rules.content, alerts: alerts.content, frequency: frequency || { interval: null, cronExpression: null, enabled: false } }))
  const refresh = () => {
    if (demoMode || !enabled) return Promise.resolve()
    setBusy(true)
    return loadWorkspace()
      .finally(() => setBusy(false))
  }
  useEffect(() => {
    if (demoMode || !enabled) return
    setBusy(true)
    loadWorkspace()
      .catch((error) => {
        if (error.message === 'SESSION_EXPIRED') {
          sessionStorage.removeItem('bridge-token')
          window.location.reload()
          return
        }
        console.error('Chargement des données impossible', error)
      }).finally(() => setBusy(false))
  }, [enabled])
  return { data, setData, busy, refresh }
}

function App() {
  const setupPath = window.location.pathname === '/setup'
  const [authenticated, setAuthenticated] = useState(demoMode || Boolean(sessionStorage.getItem('bridge-token')))
  const [currentUser, setCurrentUser] = useState(demoMode ? demoUser : null)
  const [authBusy, setAuthBusy] = useState(!demoMode && Boolean(sessionStorage.getItem('bridge-token')))
  const [authError, setAuthError] = useState('')
    const { data, setData, busy, refresh } = useWorkspace(authenticated)
  const [activePage, setActivePage] = useState('overview')
  const [mobileNav, setMobileNav] = useState(false)
  const [toast, setToast] = useState(null)
  const [modal, setModal] = useState(null)
  const [query, setQuery] = useState('')
  const [alertFilter, setAlertFilter] = useState('ALL')

  if (setupPath) return <SetupScreen onComplete={() => { window.history.replaceState({}, '', '/'); window.location.reload() }} />

  useEffect(() => {
    if (demoMode || !sessionStorage.getItem('bridge-token')) return
    request('/auth/me').then(setCurrentUser).catch(() => {
      sessionStorage.removeItem('bridge-token')
      setAuthenticated(false)
    }).finally(() => setAuthBusy(false))
  }, [])

  const notify = (message, kind = 'success') => { setToast({ message, kind }); window.setTimeout(() => setToast(null), 3200) }
  const activeAlerts = data.alerts.filter((alert) => alert.status === 'ACTIVE')
  const highRisk = data.rules.filter((rule) => rule.active && ['HIGH', 'CRITICAL'].includes(rule.severity)).length
  const filteredAlerts = data.alerts.filter((alert) => (alertFilter === 'ALL' || alert.status === alertFilter) && (!query || `${alert.id} ${alert.violatingEntityId} ${alert.involvedColumns.join(' ')}`.toLowerCase().includes(query.toLowerCase())))

  const navigate = (page) => { setActivePage(page); setMobileNav(false) }
  const saveRule = async (rule) => {
    try {
      const payload = { dslText: rule.dslText, targetTable: rule.targetTable, severity: rule.severity, active: rule.active }
      const saved = demoMode ? { ...rule, id: rule.id || Date.now() } : await request(rule.id ? `/rules/${rule.id}` : '/rules', { method: rule.id ? 'PUT' : 'POST', body: JSON.stringify(payload) })
      setData((current) => ({ ...current, rules: [saved, ...current.rules.filter((item) => item.id !== saved.id)] }))
      setModal(null); notify(rule.id ? 'Règle mise à jour' : 'Règle créée')
    } catch (error) { notify(error.message, 'info') }
  }
  const toggleScope = (name) => setData((current) => ({ ...current, scope: current.scope.includes(name) ? current.scope.filter((item) => item !== name) : [...current.scope, name] }))
  const toggleRule = async (rule) => {
    try {
      const payload = { dslText: rule.dslText, targetTable: rule.targetTable, severity: rule.severity, active: !rule.active }
      const updated = demoMode ? { ...rule, active: !rule.active } : await request(`/rules/${rule.id}`, { method: 'PUT', body: JSON.stringify(payload) })
      setData((current) => ({ ...current, rules: current.rules.map((item) => item.id === updated.id ? updated : item) }))
      notify(updated.active ? 'Règle activée' : 'Règle désactivée')
    } catch (error) { notify(error.message, 'info') }
  }
  const refreshWorkspace = async () => {
    try {
      await refresh()
      notify('Données actualisées')
    } catch (error) {
      if (error.message === 'SESSION_EXPIRED') {
        sessionStorage.removeItem('bridge-token')
        setAuthenticated(false)
        setCurrentUser(null)
        notify('Session expirée, reconnectez-vous', 'info')
        return
      }
      notify(error.message, 'info')
    }
  }

  if (!authenticated) return <LoginScreen busy={authBusy} error={authError} onSubmit={async (username, password) => {
    setAuthBusy(true); setAuthError('')
    try {
      const session = await login(username, password)
      sessionStorage.setItem('bridge-token', session.token)
      setCurrentUser(session)
      setAuthenticated(true)
    } catch (error) {
      setAuthError(error.message)
    } finally {
      setAuthBusy(false)
    }
  }} />

  return <div className="app-shell">
    <aside className={`sidebar ${mobileNav ? 'is-open' : ''}`}>
      <div className="brand"><div className="brand-mark"><Layers3 size={18} /></div><div><strong>BRIDGE</strong><span>CONTROL / BD-BANK</span></div></div>
      <div className="workspace-switcher"><span className="status-dot" /> <div><small>ESPACE ACTIF</small><strong>Conformité bancaire</strong></div><ChevronDown size={15} /></div>
      <nav className="nav-list">{nav.map(({ id, label, icon: Icon }) => <button key={id} className={`nav-item ${activePage === id ? 'active' : ''}`} onClick={() => navigate(id)}><Icon size={18} /><span>{label}</span>{id === 'alerts' && activeAlerts.length > 0 && <b>{activeAlerts.length}</b>}</button>)}</nav>
      <div className="sidebar-foot"><div className="system-pulse"><span className="pulse-ring" /><div><small>MOTEUR DE CONTRÔLE</small><strong>Opérationnel</strong></div></div><button className="nav-item" onClick={() => notify('Documentation bientôt disponible', 'info')}><CircleHelp size={18} /><span>Centre d’aide</span></button><div className="user-card"><div className="avatar">{(currentUser?.username || 'AD').slice(0, 2).toUpperCase()}</div><div><strong>{currentUser?.username || 'Administrateur'}</strong><small>{currentUser?.role || 'ADMIN'}</small></div><button aria-label="Se déconnecter" onClick={() => notify('Session déconnectée', 'info')}><LogOut size={16} /></button></div></div>
    </aside>
    {mobileNav && <button className="scrim" aria-label="Fermer le menu" onClick={() => setMobileNav(false)} />}
    <main className="main-content">
      <header className="topbar"><button className="icon-btn mobile-menu" aria-label="Ouvrir le menu" onClick={() => setMobileNav(true)}><Menu size={20} /></button><div className="breadcrumbs"><span>CONTRÔLE</span><ChevronRight size={14} /><strong>{nav.find((item) => item.id === activePage)?.label}</strong></div><div className="top-actions"><div className="connection-state"><span className="status-dot" /> API connectée</div><button className="icon-btn" aria-label="Actualiser les données" disabled={busy} onClick={refreshWorkspace}><RefreshCw size={18} className={busy ? 'spin' : ''} /></button><button className="avatar avatar-small" aria-label="Profil administrateur">{(currentUser?.username || 'AD').slice(0, 2).toUpperCase()}</button></div></header>
      <div className="page-body">
        {busy && <div className="loading-bar" />}
        {activePage === 'overview' && <Overview data={{ ...data, currentUser }} activeAlerts={activeAlerts} highRisk={highRisk} navigate={navigate} onInspect={(alert) => setModal({ type: 'alert', alert })} />}
        {activePage === 'alerts' && <Alerts alerts={filteredAlerts} filter={alertFilter} setFilter={setAlertFilter} query={query} setQuery={setQuery} onInspect={(alert) => setModal({ type: 'alert', alert })} />}
        {activePage === 'rules' && <Rules rules={data.rules} query={query} setQuery={setQuery} onCreate={() => setModal({ type: 'rule', rule: null })} onEdit={(rule) => setModal({ type: 'rule', rule })} onToggle={toggleRule} onDelete={async (id) => { try { if (!demoMode) await request(`/rules/${id}`, { method: 'DELETE' }); setData((current) => ({ ...current, rules: current.rules.filter((rule) => rule.id !== id) })); notify('Règle supprimée', 'info') } catch (error) { notify(error.message, 'info') } }} />}
        {activePage === 'schema' && <Schema data={data} toggleScope={toggleScope} onSave={async () => { try { if (!demoMode) { await request('/scope', { method: 'PUT', body: JSON.stringify(data.scope) }); await refresh() } notify('Périmètre enregistré') } catch (error) { notify(error.message, 'info') } }} />}
        {activePage === 'settings' && <Settings frequency={data.frequency} onSave={async (frequency) => { try { const saved = demoMode ? frequency : await request('/config/frequency', { method: 'PUT', body: JSON.stringify(frequency) }); setData((current) => ({ ...current, frequency: saved })); notify('Fréquence mise à jour') } catch (error) { notify(error.message, 'info') } }} />}
      </div>
    </main>
    {modal?.type === 'alert' && <AlertModal alert={modal.alert} rule={data.rules.find((rule) => rule.id === modal.alert.ruleId)} close={() => setModal(null)} />}
    {modal?.type === 'rule' && <RuleModal rule={modal.rule} tables={data.tables} close={() => setModal(null)} save={saveRule} />}
    {toast && <div className={`toast ${toast.kind}`}><span>{toast.kind === 'success' ? <Check size={16} /> : <Zap size={16} />}</span>{toast.message}</div>}
  </div>
}

function LoginScreen({ busy, error, onSubmit }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')

  const submit = (event) => {
    event.preventDefault()
    onSubmit(username.trim(), password)
  }

  return <div className="login-shell">
    <div className="login-aside">
      <div className="brand login-brand"><div className="brand-mark"><Layers3 size={18} /></div><div><strong>BRIDGE</strong><span>CONTROL / BD-BANK</span></div></div>
      <div className="login-aside-copy"><p className="eyebrow">OUTIL INTERNE / BD_BANK</p><h1>Suivre les contrôles, simplement.</h1><p>Retrouvez les anomalies détectées, vérifiez les règles en place et gardez une vue claire sur les tables surveillées.</p></div>
      <div className="login-aside-foot"><span className="status-dot" /> Infrastructure sécurisée <span>·</span> Lecture seule</div>
    </div>
    <main className="login-main">
      <div className="login-card">
        <div className="login-card-head"><p className="eyebrow">ACCÈS ADMINISTRATEUR</p><h2>Bienvenue dans Bridge Control</h2><p>Connectez-vous pour accéder à votre espace de contrôle.</p></div>
        <form onSubmit={submit}>
          <div className="form-field"><label htmlFor="username">Identifiant</label><input id="username" autoComplete="username" value={username} onChange={(event) => setUsername(event.target.value)} placeholder="Votre identifiant" required /></div>
          <div className="form-field"><div className="password-label"><label htmlFor="password">Mot de passe</label><button type="button" onClick={() => {}}>Mot de passe oublié ?</button></div><input id="password" type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="Votre mot de passe" required /></div>
          {error && <div className="login-error"><AlertTriangle size={16} /><span>{error}</span></div>}
          <button className="primary-btn login-submit" disabled={busy}>{busy ? <><RefreshCw size={16} className="spin" /> Connexion...</> : <>Ouvrir la session <ArrowUpRight size={16} /></>}</button>
        </form>
        <div className="login-note"><ShieldCheck size={15} /> Session protégée · expiration après 15 minutes d’inactivité</div>
      </div>
      <small className="login-copyright">Bridge bd_bank · Console interne de conformité</small>
    </main>
  </div>
}

function SetupScreen({ onComplete }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [setupKey, setSetupKey] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const submit = async (event) => {
    event.preventDefault(); setBusy(true); setError('')
    try { await setup(username.trim(), password, setupKey.trim()); onComplete() } catch (setupError) { setError(setupError.message) } finally { setBusy(false) }
  }
  return <div className="login-shell setup-shell"><div className="login-aside"><div className="brand login-brand"><div className="brand-mark"><Layers3 size={18} /></div><div><strong>BRIDGE</strong><span>CONTROL / BD-BANK</span></div></div><div className="login-aside-copy"><p className="eyebrow">INITIALISATION TECHNIQUE</p><h1>Un seul accès. Bien protégé.</h1><p>Ce parcours crée le compte administrateur initial. Il est désactivé dès qu’un compte existe.</p></div><div className="login-aside-foot"><span className="status-dot" /> Configuration unique <span>·</span> Accès restreint</div></div><main className="login-main"><div className="login-card"><div className="login-card-head"><p className="eyebrow">PREMIÈRE INSTALLATION</p><h2>Créer l’accès administrateur</h2><p>Cette page est réservée à l’initialisation du serveur.</p></div><form onSubmit={submit}><div className="form-field"><label htmlFor="setup-username">Identifiant administrateur</label><input id="setup-username" autoComplete="username" value={username} onChange={(event) => setUsername(event.target.value)} placeholder="admin" required /></div><div className="form-field"><label htmlFor="setup-password">Mot de passe</label><input id="setup-password" type="password" autoComplete="new-password" minLength="12" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="12 caractères minimum" required /><small>Utilisez une phrase longue, unique à cette console.</small></div><div className="form-field"><label htmlFor="setup-key">Clé d’installation serveur</label><input id="setup-key" type="password" autoComplete="off" value={setupKey} onChange={(event) => setSetupKey(event.target.value)} placeholder="Clé définie dans BDBANK_SETUP_KEY" required /></div>{error && <div className="login-error"><AlertTriangle size={16} /><span>{error}</span></div>}<button className="primary-btn login-submit" disabled={busy}>{busy ? <><RefreshCw size={16} className="spin" /> Initialisation...</> : <>Créer l’accès unique <ArrowUpRight size={16} /></>}</button></form><div className="login-note"><ShieldCheck size={15} /> Aucun lien vers cette page n’est affiché dans l’application</div></div><small className="login-copyright">Bridge bd_bank · Initialisation contrôlée</small></main></div>
}

function PageHeader({ eyebrow, title, description, action }) { return <div className="page-header"><div><p className="eyebrow">{eyebrow}</p><h1>{title}</h1><p className="lead">{description}</p></div>{action}</div> }
function StatCard({ label, value, note, tone, icon: Icon }) { return <div className={`stat-card ${tone || ''}`}><div className="stat-top"><span>{label}</span><Icon size={17} /></div><strong>{value}</strong><small>{note}</small></div> }
function Overview({ data, activeAlerts, highRisk, navigate, onInspect }) { const totalRules = data.rules.length; const activeRules = data.rules.filter((rule) => rule.active).length; const resolvedAlerts = data.alerts.filter((alert) => alert.status === 'RESOLVED').length; const totalTables = data.tables.length; const scopedTables = data.scope.length; const rulesCoverage = totalRules ? Math.round((activeRules / totalRules) * 100) : 0; const scopeCoverage = totalTables ? Math.round((scopedTables / totalTables) * 100) : 0; const alertsCoverage = data.alerts.length ? Math.round((resolvedAlerts / data.alerts.length) * 100) : 100; const score = Math.round((rulesCoverage + scopeCoverage + alertsCoverage) / 3); return <><PageHeader eyebrow="ÉTAT DU DISPOSITIF" title="Bonjour, Admin." description="Voici l’état de santé de votre dispositif de conformité." action={<button className="primary-btn" onClick={() => navigate('rules')}><Plus size={17} /> Nouvelle règle</button>} /><div className="stats-grid"><StatCard label="Alertes actives" value={activeAlerts.length.toString().padStart(2, '0')} note={`${resolvedAlerts} résolue${resolvedAlerts > 1 ? 's' : ''}`} tone="danger" icon={AlertTriangle} /><StatCard label="Règles surveillées" value={activeRules.toString().padStart(2, '0')} note={`${highRisk} à risque élevé`} icon={ShieldCheck} /><StatCard label="Tables dans le périmètre" value={`${scopedTables} / ${totalTables}`} note="Introspection bd_bank" icon={Database} /><StatCard label="Alertes traitées" value={`${alertsCoverage}%`} note={`${resolvedAlerts} / ${data.alerts.length}`} tone="ok" icon={Activity} /></div><div className="overview-grid"><section className="panel alert-panel"><div className="panel-heading"><div><p className="eyebrow">PRIORITÉ D’ACTION</p><h2>Dernières alertes</h2></div><button className="text-btn" onClick={() => navigate('alerts')}>Tout voir <ArrowUpRight size={15} /></button></div><div className="alert-list">{activeAlerts.map((alert) => <button className="alert-row" key={alert.id} onClick={() => onInspect(alert)}><span className={`severity-dot ${alertSeverity(alert, dataRules(data))}`} /><div className="alert-main"><strong>ALR-{alert.id}</strong><span>{alert.violatingEntityId} · {alert.involvedColumns.join(', ')}</span></div><div className="alert-time"><span>{formatDate(alert.detectedAt)}</span><ChevronRight size={16} /></div></button>)}</div><div className="panel-footer"><span><span className="mini-live" /> Surveillance automatique active</span><span>{activeAlerts.length} alerte{activeAlerts.length > 1 ? 's' : ''} active{activeAlerts.length > 1 ? 's' : ''}</span></div></section><section className="panel health-panel"><div className="panel-heading"><div><p className="eyebrow">COUVERTURE</p><h2>Posture de contrôle</h2></div><button className="icon-btn" aria-label="Configurer" onClick={() => navigate('settings')}><SlidersHorizontal size={17} /></button></div><div className="score"><div className="score-ring"><strong>{score}</strong><span>/100</span></div><div><strong>{score >= 80 ? 'Bonne maîtrise' : score >= 50 ? 'Maîtrise à renforcer' : 'Couverture faible'}</strong><p>Calculée selon les règles actives, le périmètre et les alertes résolues.</p></div></div><div className="health-bars"><HealthBar label="Règles actives" value={rulesCoverage} detail={`${activeRules} / ${totalRules}`} /><HealthBar label="Périmètre surveillé" value={scopeCoverage} detail={`${scopedTables} / ${totalTables}`} /><HealthBar label="Alertes résolues" value={alertsCoverage} detail={`${resolvedAlerts} / ${data.alerts.length}`} /></div><button className="outline-btn full" onClick={() => navigate('schema')}>Examiner le périmètre <ArrowUpRight size={15} /></button></section></div><section className="panel activity-panel"><div className="panel-heading"><div><p className="eyebrow">ACTIVITÉ SYSTÈME</p><h2>État des données</h2></div><span className="tag green"><span className="mini-live" /> Synchronisé</span></div><div className="cycle-track"><div className="cycle-step done"><span><Check size={14} /></span><div><strong>{activeRules} règle{activeRules > 1 ? 's' : ''} active{activeRules > 1 ? 's' : ''}</strong><small>{totalRules} règle{totalRules > 1 ? 's' : ''} configurée{totalRules > 1 ? 's' : ''}</small></div></div><div className="track-line" /><div className="cycle-step active"><span><RefreshCw size={14} /></span><div><strong>{scopedTables} table{scopedTables > 1 ? 's' : ''} surveillée{scopedTables > 1 ? 's' : ''}</strong><small>{scopeCoverage}% de couverture du schéma</small></div></div><div className="track-line faint" /><div className="cycle-step muted"><span><Settings2 size={14} /></span><div><strong>Fréquence</strong><small>{data.frequency.interval || data.frequency.cronExpression || 'Non configurée'}</small></div></div></div></section></> }

function dataRules(data) { return data.rules }
function alertSeverity(alert, rules) { return (rules.find((rule) => rule.id === alert.ruleId)?.severity || 'MEDIUM').toLowerCase() }
function HealthBar({ label, value, detail }) { return <div className="health-bar"><div><span>{label}</span><strong>{detail}</strong></div><div className="bar"><i style={{ width: `${value}%` }} /></div></div> }
function Alerts({ alerts, filter, setFilter, query, setQuery, onInspect }) { return <><PageHeader eyebrow="CENTRE DE RÉSOLUTION" title="Alertes" description="Suivez les anomalies détectées par les règles de conformité." action={<button className="outline-btn" onClick={() => setFilter('ACTIVE')}><SlidersHorizontal size={16} /> Filtrer les actives</button>} /><div className="toolbar"><div className="search-field"><Search size={17} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Rechercher par identifiant ou colonne" /></div><div className="segmented">{[['ALL', 'Toutes'], ['ACTIVE', 'Actives'], ['RESOLVED', 'Résolues']].map(([key, label]) => <button key={key} className={filter === key ? 'selected' : ''} onClick={() => setFilter(key)}>{label}<span>{key === 'ALL' ? alerts.length : alerts.filter((alert) => alert.status === key).length}</span></button>)}</div></div><section className="panel table-panel"><div className="table-scroll"><table><thead><tr><th>Statut</th><th>Alerte</th><th>Entité concernée</th><th>Colonnes impliquées</th><th>Détection</th><th /></tr></thead><tbody>{alerts.map((alert) => <tr key={alert.id} onClick={() => onInspect(alert)}><td><span className={`status-tag ${alert.status.toLowerCase()}`}><i />{alert.status === 'ACTIVE' ? 'Active' : 'Résolue'}</span></td><td><strong>ALR-{alert.id}</strong><small>Règle #{alert.ruleId}</small></td><td><code>{alert.violatingEntityId}</code></td><td><div className="chips">{alert.involvedColumns.map((column) => <span key={column}>{column}</span>)}</div></td><td>{formatDate(alert.detectedAt)}</td><td><ChevronRight size={17} /></td></tr>)}{alerts.length === 0 && <tr><td colSpan="6" className="empty-state">Aucune alerte ne correspond à votre recherche.</td></tr>}</tbody></table></div><div className="table-footer"><span>Affichage de {alerts.length} alertes</span><div><button className="icon-btn" aria-label="Page précédente"><ChevronRight size={16} className="flip" /></button><span className="page-number">1</span><button className="icon-btn" aria-label="Page suivante"><ChevronRight size={16} /></button></div></div></section></> }

function Rules({ rules, query, setQuery, onCreate, onEdit, onToggle, onDelete }) { const visible = rules.filter((rule) => `${rule.dslText} ${rule.targetTable}`.toLowerCase().includes(query.toLowerCase())); return <><PageHeader eyebrow="BIBLIOTHÈQUE DE CONTRÔLES" title="Règles DSL" description="Écrivez, testez et maintenez les contrôles métier de bd_bank." action={<button className="primary-btn" onClick={onCreate}><Plus size={17} /> Créer une règle</button>} /><div className="toolbar"><div className="search-field"><Search size={17} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Rechercher une règle ou une table" /></div><span className="toolbar-note"><ShieldCheck size={16} /> Validation syntaxique disponible à la saisie</span></div><section className="panel rules-panel"><div className="rule-table-head"><span>RÈGLE / CIBLE</span><span>GRAVITÉ</span><span>ÉTAT</span><span>CRÉÉE LE</span><span /></div>{visible.map((rule) => <div className="rule-row" key={rule.id}><div className="rule-identity"><span className="rule-icon"><KeyRound size={16} /></span><div><strong>{rule.dslText}</strong><small><Table2 size={13} /> {rule.targetTable} <span className="dot-separator">·</span> Règle #{rule.id}</small></div></div><span className={`severity-label ${rule.severity.toLowerCase()}`}><i />{labels[rule.severity]}</span><span className={`active-label ${rule.active ? 'on' : ''}`}><i />{rule.active ? 'Active' : 'En pause'}</span><small className="date-label">{formatDate(rule.createdAt)}</small><div className="row-actions"><button className="icon-btn" aria-label={rule.active ? 'Désactiver la règle' : 'Activer la règle'} onClick={() => onToggle(rule)}>{rule.active ? <X size={16} /> : <Check size={16} />}</button><button className="icon-btn" aria-label="Modifier la règle" onClick={() => onEdit(rule)}><Settings2 size={16} /></button><button className="icon-btn danger-icon" aria-label="Supprimer la règle" onClick={() => onDelete(rule.id)}><X size={16} /></button></div></div>)}</section></> }

function Schema({ data, toggleScope, onSave }) { const [selected, setSelected] = useState(data.tables[0]?.name); return <><PageHeader eyebrow="LECTURE DU SCHÉMA" title="Schéma & périmètre" description="Choisissez précisément les tables que le moteur doit surveiller." action={<button className="primary-btn" onClick={onSave}><Check size={17} /> Enregistrer le périmètre</button>} /><div className="schema-layout"><section className="panel schema-list"><div className="panel-heading"><div><p className="eyebrow">{data.tables.length} TABLES DÉTECTÉES</p><h2>Tables disponibles</h2></div><Database size={19} /></div>{data.tables.map((table) => <button className={`schema-item ${selected === table.name ? 'selected' : ''}`} key={table.name} onClick={() => setSelected(table.name)}><span className={`table-check ${data.scope.includes(table.name) ? 'checked' : ''}`} onClick={(event) => { event.stopPropagation(); toggleScope(table.name) }}>{data.scope.includes(table.name) && <Check size={13} />}</span><div><strong>{table.name}</strong><small>{table.catalog} · table relationnelle</small></div><ChevronRight size={16} /></button>)}</section><section className="panel columns-panel"><div className="panel-heading"><div><p className="eyebrow">INTROSPECTION</p><h2>{selected}</h2></div><span className="tag">{data.scope.includes(selected) ? 'Dans le périmètre' : 'Hors périmètre'}</span></div><div className="column-grid">{(selected === 'clients' ? [['id_client', 'BIGINT'], ['nom', 'VARCHAR(120)'], ['email', 'VARCHAR(180)'], ['date_creation', 'DATETIME']] : selected === 'comptes' ? [['id_compte', 'BIGINT'], ['id_client', 'BIGINT'], ['solde', 'DECIMAL(14,2)'], ['decouvert_autorise', 'BOOLEAN']] : selected === 'transactions' ? [['id_transaction', 'BIGINT'], ['id_compte', 'BIGINT'], ['montant', 'DECIMAL(14,2)'], ['date_operation', 'DATETIME']] : [['id_agence', 'BIGINT'], ['nom', 'VARCHAR(100)'], ['ville', 'VARCHAR(100)']]).map(([name, type]) => <div className="column-item" key={name}><span><Table2 size={15} />{name}</span><code>{type}</code></div>)}</div><div className="privacy-note"><ShieldCheck size={18} /><div><strong>Données protégées par conception</strong><p>Les alertes ne remontent que l’identifiant de la ligne et les colonnes impliquées. Aucune valeur bancaire complète n’est exposée.</p></div></div></section></div></> }
function Settings({ frequency, onSave }) { const [mode, setMode] = useState(frequency.cronExpression ? 'cron' : 'interval'); const [interval, setInterval] = useState(frequency.interval || '5m'); const [cron, setCron] = useState(frequency.cronExpression || '0 */5 * * * *'); return <><PageHeader eyebrow="PARAMÈTRES D’EXÉCUTION" title="Configuration" description="Cadencez le moteur de conformité selon le rythme de votre activité." /><div className="settings-grid"><section className="panel setting-card"><div className="panel-heading"><div><p className="eyebrow">SCHEDULER GLOBAL</p><h2>Fréquence d’analyse</h2></div><Activity size={19} /></div><div className="setting-control"><label>Mode d’exécution</label><div className="segmented wide"><button className={mode === 'interval' ? 'selected' : ''} onClick={() => setMode('interval')}>Intervalle simple</button><button className={mode === 'cron' ? 'selected' : ''} onClick={() => setMode('cron')}>Expression cron</button></div></div>{mode === 'interval' ? <div className="setting-control"><label htmlFor="interval">Intervalle</label><select id="interval" value={interval} onChange={(event) => setInterval(event.target.value)}><option value="3m">Toutes les 3 minutes</option><option value="5m">Toutes les 5 minutes</option><option value="15m">Toutes les 15 minutes</option><option value="1h">Toutes les heures</option></select><small>Minimum imposé par la politique de conformité : 3 minutes.</small></div> : <div className="setting-control"><label htmlFor="cron">Expression cron</label><input id="cron" value={cron} onChange={(event) => setCron(event.target.value)} /><small>Exemple : 0 */5 * * * * pour une exécution toutes les 5 minutes.</small></div>}<button className="primary-btn" onClick={() => onSave(mode === 'interval' ? { interval, cronExpression: null, enabled: 'true' } : { interval: null, cronExpression: cron, enabled: 'true' })}><Check size={17} /> Appliquer la configuration</button></section><section className="panel run-card"><div className="run-illustration"><div className="orbit orbit-one" /><div className="orbit orbit-two" /><div className="run-core"><Play size={21} fill="currentColor" /></div></div><p className="eyebrow">ÉTAT DU SERVICE</p><h2>Analyse automatique active</h2><p>Le moteur exécute les {frequency.interval ? `contrôles toutes les ${frequency.interval.replace('m', ' minutes')}` : 'contrôles selon votre expression cron'}.</p><div className="run-meta"><span><span className="status-dot" /> Prochain cycle</span><strong>{frequency.nextCycleAt ? formatTime(frequency.nextCycleAt) : 'N/A'}</strong></div></section></div></> }

function AlertModal({ alert, rule, close }) {
  return <div className="modal-backdrop" onClick={close}><div className="modal" onClick={(event) => event.stopPropagation()}><div className="modal-head"><div><p className="eyebrow">DÉTAIL DE L’ALERTE</p><h2>ALR-{alert.id}</h2></div><button className="icon-btn" aria-label="Fermer" onClick={close}><X size={19} /></button></div><div className="modal-status"><span className={`status-tag ${alert.status.toLowerCase()}`}><i />{alert.status === 'ACTIVE' ? 'Active' : 'Résolue'}</span><span>Détectée le {formatDate(alert.detectedAt)}</span></div><div className="detail-block"><span>Règle déclenchée</span><strong>{rule?.dslText || `Règle #${alert.ruleId}`}</strong><small>Table cible : {rule?.targetTable || 'Non renseignée'}</small></div><div className="detail-grid"><div><span>Identifiant concerné</span><code>{alert.violatingEntityId}</code></div><div><span>Détections consécutives</span><strong>{alert.consecutiveDetections} cycles</strong></div></div><div className="detail-block"><span>Colonnes impliquées</span><div className="chips">{alert.involvedColumns.map((column) => <span key={column}>{column}</span>)}</div></div><div className="privacy-note compact"><ShieldCheck size={17} /><p>Les valeurs réelles sont masquées pour respecter la confidentialité des données bancaires.</p></div></div></div>
}

function RuleModal({ rule, tables, close, save }) { const [draft, setDraft] = useState(rule || { dslText: '', targetTable: tables[0]?.name || '', severity: 'MEDIUM', active: true }); const [valid, setValid] = useState(null); const update = (key, value) => setDraft((current) => ({ ...current, [key]: value })); const validate = () => setValid(draft.dslText.trim().length > 5 && /[<>=]|IS NULL|COUNT|SUM|AVG|MAX|MIN/.test(draft.dslText) ? true : false); return <div className="modal-backdrop" onClick={close}><div className="modal rule-modal" onClick={(event) => event.stopPropagation()}><div className="modal-head"><div><p className="eyebrow">{rule ? 'MODIFIER LE CONTRÔLE' : 'NOUVEAU CONTRÔLE'}</p><h2>Règle DSL</h2></div><button className="icon-btn" aria-label="Fermer" onClick={close}><X size={19} /></button></div><div className="form-field"><label htmlFor="dsl">Expression métier</label><textarea id="dsl" value={draft.dslText} onChange={(event) => { update('dslText', event.target.value); setValid(null) }} placeholder="ex. solde < 0 AND decouvert_autorise = false" rows="3" />{valid !== null && <small className={valid ? 'valid-hint' : 'error-hint'}>{valid ? 'Syntaxe de base valide. Le backend vérifiera les colonnes.' : 'Ajoutez une comparaison ou une fonction d’agrégat valide.'}</small>}</div><div className="form-grid"><div className="form-field"><label htmlFor="target">Table cible</label><select id="target" value={draft.targetTable} onChange={(event) => update('targetTable', event.target.value)}>{tables.map((table) => <option key={table.name}>{table.name}</option>)}</select></div><div className="form-field"><label htmlFor="severity">Gravité</label><select id="severity" value={draft.severity} onChange={(event) => update('severity', event.target.value)}>{Object.entries(labels).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select></div></div><label className="toggle-line"><input type="checkbox" checked={draft.active} onChange={(event) => update('active', event.target.checked)} /><span className="toggle" /> Activer après enregistrement</label><div className="modal-actions"><button className="outline-btn" onClick={validate}><Check size={16} /> Valider la syntaxe</button><button className="primary-btn" disabled={!draft.dslText.trim()} onClick={() => save(draft)}>{rule ? 'Enregistrer' : 'Créer la règle'} <ArrowUpRight size={16} /></button></div></div></div> }

createRoot(document.getElementById('root')).render(<App />)
