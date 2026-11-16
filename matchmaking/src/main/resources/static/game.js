/* Compact Matchmaking UI; reads native Timefold ScoreAnalysis only.
 * GET /mm and GET /mm/score (the latter must use ScoreAnalysisFetchPolicy.FETCH_ALL).
 */
const params = new URLSearchParams(location.search);
const gameId = params.get('id');
const GAME_URL = gameId ? `/mm/${encodeURIComponent(gameId)}` : '/mm';
const SCORE_URL = gameId ? `/mm/score/${encodeURIComponent(gameId)}` : '/mm/score';

let game = null, analysis = null, entityIndex = new Map(), fullyAnalyzed = false;
let otherContributors = [];
const TOTAL_SELECTION = '__total_score__';
let selected = TOTAL_SELECTION, selectedConstraint = null, loading = false, poller = null;
const $ = id => document.getElementById(id);
const escapeHtml = v => String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const items = a => Array.isArray(a) ? a : [];
const refId = v => v == null ? null : (typeof v === 'string' || typeof v === 'number' ? String(v) : v.id ?? v.playerId ?? v.teamId ?? v.partyId ?? v.roleId ?? null);
const role = r => refId(r) ?? '?';
const entityKey = (kind,id) => `${kind}:${id}`;
const roleCounts = roles => {
  const counts = new Map();
  items(roles).forEach(r => counts.set(role(r), (counts.get(role(r)) || 0) + 1));
  return [...counts].map(([name,count]) => `${name}${count > 1 ? '×'+count : ''}`).join(' ');
};
const scoreText = value => {
  if (value == null) return '—';
  if (typeof value === 'string' || typeof value === 'number') return String(value);
  if ('hardScore' in value || 'softScore' in value) return `${value.hardScore ?? 0}hard/${value.softScore ?? 0}soft`;
  return JSON.stringify(value);
};
function scoreParts(value) {
  if (value == null) return null;
  if (typeof value === 'object' && ('hardScore' in value || 'softScore' in value)) {
    return {hard:Number(value.hardScore ?? 0),soft:Number(value.softScore ?? 0)};
  }
  const s = scoreText(value);
  const hard = s.match(/(-?\d+(?:\.\d+)?)hard/), soft = s.match(/(-?\d+(?:\.\d+)?)soft/);
  if (!hard && !soft) return null;
  return {hard:hard ? Number(hard[1]) : 0,soft:soft ? Number(soft[1]) : 0};
}
const level = score => {
  const p = scoreParts(score);
  return !p || !Number.isFinite(p.hard) || !Number.isFinite(p.soft) ? 'unknown' :
    p.hard !== 0 ? 'hard' : p.soft !== 0 ? 'soft' : 'clean';
};
const rank = {hard:0,soft:1,unknown:2,clean:3};
const listText = data => typeof data === 'string' ? data : JSON.stringify(data, null, 2);
function setWarning(id,message){$(id).textContent=message;$(id).hidden=!message;}
async function fetchJson(url) {
  const response = await fetch(url,{cache:'no-store',headers:{Accept:'application/json'}});
  if (!response.ok) throw new Error(`${response.status} ${response.statusText} from ${url}`);
  return response.json();
}

// Timefold FETCH_ALL serializes indictments as groups:
//   indictments: [{ type: "...domain.Player", analyses: [
//       { indictee: "P 1" | {id: "P 1", ...}, score: "...", matches: [...] }
//   ] }]
// Also accept older/map variants (indictmentMap) without recomputing scores.
// An absent indictment in a *complete* response is a zero contribution.
function collectIndictments(data) {
  const index = new Map();
  otherContributors = [];
  fullyAnalyzed = false;
  const raw = data?.indictments ?? data?.indictmentMap;
  if (raw == null) return index;

  const playerIds = new Set(items(game?.playerList).map(refId));
  const teamIds = new Set(items(game?.teamList).map(refId));
  let actualEntries = 0;
  const unnamedCounts = new Map();
  const typeName = value => String(value || '').split('.').pop().split('$').pop();

  const accept = (entry, declaredType, fallbackId) => {
    if (!entry || typeof entry !== 'object' || !('score' in entry)) return;
    const indictee = entry.indictee ?? entry.indictedObject ?? entry.object ?? fallbackId;
    const id = refId(indictee);
    let kind = typeName(declaredType ?? entry.type ?? entry.indictmentClassId);
    if (!kind) {
      const isPlayer = playerIds.has(id), isTeam = teamIds.has(id);
      if (isPlayer !== isTeam) kind = isPlayer ? 'Player' : 'Team';
    }
    if (!kind) return;
    // Some facts such as MatchmakingConfig have no id property. Give them
    // synthetic UI keys, but never confuse them with Team/Player IDs.
    let resolvedId = id;
    if (resolvedId == null) {
      if (kind === 'Team' || kind === 'Player') return;
      const count = (unnamedCounts.get(kind) || 0) + 1;
      unnamedCounts.set(kind, count);
      resolvedId = `#${count}`;
    }
    const key = entityKey(kind, resolvedId);
    const matches = items(entry.matches ?? entry.matchAnalyses);
    const previous = index.get(key);
    if (previous) {
      const a = scoreParts(previous.score), b = scoreParts(entry.score);
      previous.score = a && b ? {hardScore:a.hard + b.hard, softScore:a.soft + b.soft} : null;
      previous.matches.push(...matches);
      previous.count = previous.count != null && entry.matchCount != null
        ? previous.count + entry.matchCount : null;
    } else {
      const record = {score:entry.score, matches:[...matches],
        count:entry.matchCount ?? null, indictee, kind, id:resolvedId, key};
      index.set(key, record);
      if (kind !== 'Team' && kind !== 'Player') otherContributors.push(record);
    }
    actualEntries++;
  };

  // Format returned by the user's /mm/score endpoint.
  if (Array.isArray(raw)) {
    raw.forEach(group => {
      if (group && typeof group === 'object' && Array.isArray(group.analyses)) {
        group.analyses.forEach(entry => accept(entry, group.type ?? group.indictmentClassId));
      } else if (group && typeof group === 'object' && 'score' in group) {
        accept(group, group.type ?? group.indictmentClassId);
      }
    });
  } else if (typeof raw === 'object') {
    Object.entries(raw).forEach(([classId, group]) => {
      if (Array.isArray(group)) group.forEach(entry => accept(entry, classId));
      else if (Array.isArray(group?.analyses)) group.analyses.forEach(entry => accept(entry, group.type ?? classId));
      else if (group && typeof group === 'object' && 'score' in group) accept(group, classId);
      else if (group && typeof group === 'object')
        Object.entries(group).forEach(([id, entry]) => accept(entry, classId, id));
    });
  }

  // Nonempty valid indictments, even for non-entity fact types, indicate
  // that entity-level attribution exists. An empty response with a nonzero
  // game score is ambiguous: keep the UI gray instead of falsely green.
  const total = scoreParts(data?.score);
  const zeroScore = total && total.hard === 0 && total.soft === 0;
  fullyAnalyzed = actualEntries > 0 || Boolean(zeroScore);
  return index;
}
function ownState(kind,id){
  if (!fullyAnalyzed) return {severity:'unknown',score:null,entry:null};
  const entry = entityIndex.get(entityKey(kind,id)) ?? null;
  // Absence from a complete FETCH_ALL indictment map means no attributed contribution.
  return {severity:entry ? level(entry.score) : 'clean', score:entry?.score ?? '0hard/0soft',entry};
}
function severityForTeam(team,members){
  const levels = [ownState('Team',refId(team)).severity, ...members.map(p => ownState('Player',refId(p)).severity)];
  return levels.reduce((best,current) => rank[current] < rank[best] ? current : best,'clean');
}
function entityButton(kind,id,label,extra=''){
  const state=ownState(kind,id), key=entityKey(kind,id);
  const title=`${kind} ${id}: ${state.severity === 'unknown' ? 'score contribution unavailable' : scoreText(state.score)}`;
  return `<button type="button" class="entity ${state.severity}${selected===key?' selected':''} ${extra}" data-kind="${kind}" data-id="${escapeHtml(id)}" title="${escapeHtml(title)}" aria-label="${escapeHtml(title)}" aria-pressed="${selected===key}"><i class="dot ${state.severity}" aria-hidden="true"></i>${label}</button>`;
}
function playerButton(p){
  const id=refId(p);
  return entityButton('Player',id,`<strong>${escapeHtml(id)}</strong> <span class="role">${escapeHtml(role(p.role))}</span>`,'player-chip');
}
function teamMembers(team,byTeam){
  const members=byTeam.get(refId(team)) ?? [];
  const order = new Map(items(team.players).map((p,i)=>[refId(p),i]));
  return [...members].sort((a,b)=>(order.get(refId(a))??1e8)-(order.get(refId(b))??1e8));
}
function refreshTeamList(){
  if(!game)return;
  const search=$('search').value.trim().toLowerCase(), filter=$('severity-filter').value, sort=$('sort-order').value;
  const byTeam=new Map();
  items(game.teamList).forEach(t=>byTeam.set(refId(t),[]));
  items(game.playerList).forEach(p=>{const team=refId(p.assignedTeam);if(byTeam.has(team))byTeam.get(team).push(p);});
  let rows=items(game.teamList).map((team,index)=>{
    const members=teamMembers(team,byTeam);
    return {team,members,index,severity:severityForTeam(team,members)};
  });
  rows=rows.filter(({team,members,severity})=>{
    if(filter!=='all'&&filter!==severity)return false;
    if(!search)return true;
    return [refId(team),refId(team.opponent),...members.flatMap(p=>[refId(p),refId(p.party),role(p.role)])]
      .some(v=>String(v??'').toLowerCase().includes(search));
  });
  if(sort==='severity') rows.sort((a,b)=>rank[a.severity]-rank[b.severity]||a.index-b.index);
  if(sort==='name') rows.sort((a,b)=>String(refId(a.team)).localeCompare(String(refId(b.team)),undefined,{numeric:true}));
  $('visible-count').textContent=`(${rows.length}/${items(game.teamList).length})`;
  $('team-list').innerHTML=rows.length? rows.map(({team,members})=>{
    const id=refId(team),needed=roleCounts(team.requiredRoles),available=roleCounts(team.availableRoles);
    return `<div class="team-row">
      <div>${entityButton('Team',id,`<span class="team-id">${escapeHtml(id)}</span>`)}</div>
      <div class="team-opponent">${escapeHtml(refId(team.opponent)??'—')}</div>
      <div class="team-numbers" title="Required: ${escapeHtml(needed||'—')} | Available: ${escapeHtml(available||'—')}"><strong>${escapeHtml(team.averageRating??'—')}</strong> MMR<div class="team-meta">${escapeHtml(members.length)}/${escapeHtml(team.nrOfPlayers??'?')} players · roles ↗</div></div>
      <div class="players">${members.map(playerButton).join('')||'<span class="muted small">No players</span>'}</div>
    </div>`;
  }).join(''):'<div class="empty" style="padding:12px">No teams match the filters.</div>';
  const waiting=items(game.playerList).filter(p=>refId(p.assignedTeam)==null);
  $('waiting').hidden=waiting.length===0;
  $('waiting-count').textContent=`(${waiting.length})`;
  $('waiting-players').innerHTML=waiting.map(playerButton).join('');
}
function constraints(){
  if(!analysis)return [];
  const source=analysis.constraintAnalyses??analysis.constraintMap??analysis.constraints??[];
  const array=Array.isArray(source)?source:Object.entries(source).map(([key,value])=>({...value,_key:key}));
  return array.map((c,i)=>({
    id:c.id??c.constraintId??c.constraintRef?.constraintId??c.constraintRef?.id??c._key??c.name??`Constraint ${i+1}`,
    score:c.score,count:c.matchCount,matches:c.matches??c.matchAnalyses??null
  })).sort((a,b)=>rank[level(a.score)]-rank[level(b.score)]||String(a.id).localeCompare(String(b.id)));
}
function renderConstraints(){
  const c=constraints();
  $('constraint-list').innerHTML=c.length?c.map((row,index)=>{
    return `<div class="analysis-row"><button type="button" data-constraint="${index}">${escapeHtml(row.id)}</button><span class="mono ${level(row.score)==='hard'?'score-hard':level(row.score)==='soft'?'score-soft':''}" title="${escapeHtml(row.count??'—')} matches">${escapeHtml(scoreText(row.score))}</span></div>`;
  }).join(''):'<div class="empty">No constraint analysis in the response.</div>';
  renderConstraintDetails();
}
function renderConstraintDetails(){
  const c=constraints(), row=selectedConstraint==null?null:c[selectedConstraint];
  if(!row){$('constraint-details').innerHTML='';return;}
  const matches=Array.isArray(row.matches)?row.matches:null;
  $('constraint-details').innerHTML=`<details open><summary><strong>${escapeHtml(row.id)}</strong> · ${escapeHtml(row.count??matches?.length??'—')} matches</summary>`+
    (matches===null?'<p class="empty">Match details not fetched. Use FETCH_ALL.</p>':matches.length===0?'<p class="empty">No matches.</p>':
      matches.slice(0,100).map(m=>`<div class="match"><b>${escapeHtml(scoreText(m.score))}</b><pre>${escapeHtml(listText(m.justification??m))}</pre></div>`).join('')+
      (matches.length>100?`<p class="empty">Showing first 100 of ${matches.length} matches.</p>`:''))+'</details>';
}
function renderOtherContributors() {
  const panel = $('other-contributors');
  panel.hidden = !otherContributors.length;
  $('other-list').innerHTML = otherContributors.map(entry =>
    entityButton(entry.kind, entry.id,
      `<strong>${escapeHtml(entry.kind)}</strong> <span class="role">${escapeHtml(scoreText(entry.score))}</span>`,
      'other-chip')
  ).join('');
}
function renderSelected(){
  const showingTotal=selected===TOTAL_SELECTION;
  $('detail-heading').textContent=showingTotal?'Constraints · Total score':'Selected object';
  $('entity-details').hidden=showingTotal;
  $('total-details').hidden=!showingTotal;
  if(showingTotal){
    const score=analysis?.score??game?.score;
    const severity=level(score);
    $('total-score-summary').innerHTML=`<span class="entity ${severity}"><i class="dot ${severity}"></i><b>${escapeHtml(scoreText(score))}</b></span>`+
      `<span class="score-help">${!analysis?'Constraint analysis unavailable': 'Native Timefold constraint contributions'}</span>`;
    return;
  }
  if(!selected){$('entity-details').innerHTML='<p class="empty">Click any colored team or player.</p>';return;}
  const at=selected.indexOf(':'),kind=selected.slice(0,at),id=selected.slice(at+1),state=ownState(kind,id);
  const obj = kind === 'Team' ? items(game?.teamList).find(t => refId(t) === id)
    : kind === 'Player' ? items(game?.playerList).find(p => refId(p) === id)
    : state.entry?.indictee;

  const facts = kind === 'Team' && obj
    ? `Opponent: ${escapeHtml(refId(obj.opponent)??'—')} · Average MMR: ${escapeHtml(obj.averageRating??'—')}<br>Required roles: ${escapeHtml(roleCounts(obj.requiredRoles)||'—')}<br>Available roles: ${escapeHtml(roleCounts(obj.availableRoles)||'—')}`
    : kind === 'Player' && obj
    ? `Team: ${escapeHtml(refId(obj.assignedTeam)??'unassigned')} · Role: ${escapeHtml(role(obj.role))}<br>Rating: ${escapeHtml(obj.rating??'—')} · Waiting: ${escapeHtml(obj.waitingTime??'—')}s · Party: ${escapeHtml(refId(obj.party)??'—')}`
    : obj && typeof obj === 'object'
    ? `<pre>${escapeHtml(listText(obj))}</pre>` : '';
  const heading=`<div style="margin:8px 0"><strong>${escapeHtml(kind)} ${escapeHtml(id)}</strong><br><span class="entity ${state.severity}" style="margin-top:7px"><i class="dot ${state.severity}"></i>${escapeHtml(state.severity.toUpperCase())}: ${escapeHtml(state.score===null?'Unknown':scoreText(state.score))}</span><div class="small muted" style="margin-top:7px">${facts}</div></div>`;
  if(!fullyAnalyzed){$('entity-details').innerHTML=heading+'<p class="empty">No native indictment data. Request FETCH_ALL from Timefold.</p>';return;}
  const entry=state.entry;
  if(!entry){$('entity-details').innerHTML=heading+'<p class="empty">No attributed contribution for this object.</p>';return;}
  const matches=entry.matches;
  $('entity-details').innerHTML=heading+`<p class="small muted">${escapeHtml(entry.count??matches.length)} matches (native Timefold indictments)</p>`+
    (matches.length ? matches.slice(0,100).map(m=>`<div class="match"><b>${escapeHtml(m.constraint??m.constraintId??m.constraintRef?.constraintId??'Constraint match')} · ${escapeHtml(scoreText(m.score))}</b><pre>${escapeHtml(listText(m.justification??m))}</pre></div>`).join('') :
      '<p class="empty">Indictment score is available; individual match details were not returned.</p>')+
    (matches.length>100?`<p class="empty">Showing first 100 of ${matches.length} matches.</p>`:'');
}
function renderAll(){
  if(!game)return;
  $('game-name').textContent=game.name??'Game';
  const players=items(game.playerList),teams=items(game.teamList),waiting=players.filter(p=>refId(p.assignedTeam)==null);
  const totalScore=analysis?.score??game.score, totalSeverity=level(totalScore);
  const stats=[['Solver',game.solverStatus??'—'],['Teams',teams.length],['Assigned',players.length-waiting.length],['Waiting',waiting.length]];
  $('stats').innerHTML=`<button type="button" class="stat entity score-stat ${totalSeverity}${selected===TOTAL_SELECTION?' selected':''}" id="total-score-btn" data-select="total" aria-pressed="${selected===TOTAL_SELECTION}" title="View all constraint contributions"><i class="dot ${totalSeverity}" aria-hidden="true"></i><span>Total score:</span><b>${escapeHtml(scoreText(totalScore))}</b></button>`+
    stats.map(([k,v])=>`<span class="stat">${escapeHtml(k)}: <b>${escapeHtml(v)}</b></span>`).join('');
  $('analysis-status').textContent=fullyAnalyzed?'Native Timefold indictments · FETCH_ALL':'Indictment details unavailable';
  setWarning('analysis-warning',!analysis?`Score analysis unavailable. Expected GET ${SCORE_URL}.`:
    !fullyAnalyzed?'ScoreAnalysis has no usable indictment data. Request FETCH_ALL; gray means unknown (an empty map alone is not proof of no penalties).':'');
  refreshTeamList();renderOtherContributors();renderSelected();renderConstraints();
}
async function refresh(){
  if(loading)return;loading=true;$('refresh-btn').disabled=true;setWarning('error','');
  try{
    game=await fetchJson(GAME_URL);
    try {analysis=await fetchJson(SCORE_URL);}catch(e){analysis=null;console.warn('ScoreAnalysis:',e.message);}
    entityIndex=collectIndictments(analysis);
    renderAll();
  }catch(e){setWarning('error',`Unable to load game: ${e.message}`);}
  finally{loading=false;$('refresh-btn').disabled=false;}
}
// Event delegation means we register only one click handler even for 100+ teams.
$('team-list').addEventListener('click',selectEntity);
$('waiting-players').addEventListener('click',selectEntity);
$('other-list').addEventListener('click',selectEntity);
function updateSelection(){
  document.querySelectorAll('button.entity').forEach(button=>{
    const active=button.dataset.select==='total' ? selected===TOTAL_SELECTION :
      selected===entityKey(button.dataset.kind,button.dataset.id);
    button.classList.toggle('selected',active);
    button.setAttribute('aria-pressed',String(active));
  });
}
function selectEntity(event){
  const el=event.target.closest('button[data-kind]');if(!el)return;
  selected=entityKey(el.dataset.kind,el.dataset.id);
  updateSelection();renderSelected();
}
$('stats').addEventListener('click',event=>{
  if(!event.target.closest('[data-select="total"]'))return;
  selected=TOTAL_SELECTION;
  updateSelection();renderSelected();
});
$('constraint-list').addEventListener('click',event=>{
  const el=event.target.closest('[data-constraint]');if(!el)return;
  selectedConstraint=Number(el.dataset.constraint);renderConstraintDetails();
});
$('search').addEventListener('input',refreshTeamList);
$('severity-filter').addEventListener('change',refreshTeamList);
$('sort-order').addEventListener('change',refreshTeamList);
$('refresh-btn').addEventListener('click',refresh);
$('auto-refresh').addEventListener('change',e=>{
  if(poller)clearInterval(poller);
  poller=e.target.checked?setInterval(refresh,3000):null;
});
refresh();
