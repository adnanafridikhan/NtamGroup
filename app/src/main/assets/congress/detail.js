"use strict";

const screen = document.body.dataset.screen;
const all = (selector, root = document) => [...root.querySelectorAll(selector)];
const button = label => all("button[aria-label]").find(element => element.getAttribute("aria-label").toLowerCase() === label.toLowerCase());
const text = element => element.textContent.trim().replace(/\s+/g, " ");
const open = action => { location.href = "congress://action/" + action; };
const normalize = value => value.trim().toLowerCase();
const storageKey = name => "congress:" + name;
const read = (name, fallback) => {
  try {
    const value = JSON.parse(localStorage.getItem(storageKey(name)));
    return value === null ? fallback : value;
  } catch (error) {
    return fallback;
  }
};
const save = (name, value) => {
  try {
    localStorage.setItem(storageKey(name), JSON.stringify(value));
  } catch (error) {
    // Keep the current screen usable when storage is unavailable.
  }
};

let statusTimeout;
function notify(message) {
  let status = document.querySelector(".detail-status");
  if (!status) {
    status = document.createElement("div");
    status.className = "detail-status";
    status.setAttribute("role", "status");
    document.body.appendChild(status);
  }
  status.textContent = message;
  clearTimeout(statusTimeout);
  statusTimeout = setTimeout(() => { status.remove(); }, 3500);
}
function bind(label, callback) {
  const target = button(label);
  if (target) target.addEventListener("click", callback);
}
function clickable(element, callback) {
  element.tabIndex = 0;
  element.setAttribute("role", "button");
  element.addEventListener("click", callback);
  element.addEventListener("keydown", event => {
    if (event.target === element && (event.key === "Enter" || event.key === " ")) {
      event.preventDefault();
      callback(event);
    }
  });
}
function chipGroup(chips, initial, change) {
  function select(label) {
    chips.forEach(chip => chip.setAttribute("aria-pressed", String(text(chip) === label)));
    change(label);
  }
  chips.forEach(chip => {
    chip.classList.add("detail-chip");
    chip.addEventListener("click", () => select(text(chip)));
  });
  select(initial);
  return select;
}
function filterList(input, chips, cards, matches) {
  let category = "All";
  const empty = document.createElement("p");
  empty.className = "detail-empty";
  empty.setAttribute("role", "status");
  empty.textContent = "No matching results.";
  cards[0].parentElement.appendChild(empty);
  function update() {
    const query = normalize(input.value);
    cards.forEach((card, index) => {
      card.hidden = !normalize(text(card)).includes(query) || !matches(category, index);
    });
    empty.hidden = cards.some(card => !card.hidden);
  }
  input.setAttribute("aria-label", input.placeholder);
  input.addEventListener("input", update);
  const select = chipGroup(chips, category, value => { category = value; update(); });
  return { select, update };
}

bind("Go back", () => open("back"));
document.addEventListener("click", event => {
  const anchor = event.target.closest('a[href^="congress://action/"]');
  if (anchor) {
    event.preventDefault();
    open(new URL(anchor.href).pathname.slice(1));
  }
});

function setupQuickAccess() {
  const cards = all("article");
  const extra = [
    ["Voting", "Respond to live polls", "Interactive", "voting", "M5 20v-5m7 5V4m7 16V9"],
    ["Ask Q&A", "Ask the congress experts", "Interactive", "ask-qa", "M3 4h18v13H8l-5 4z"],
    ["Resources", "Browse the congress library", "Tools", "resources", "M3 6h7l2 3h9v11H3z"],
    ["Exhibitors", "Explore the exhibitor directory", "Event", "exhibitors", "M3 8h18v12H3zM8 8V4h8v4"]
  ];
  for (const [name, description, category, route, path] of extra) {
    const article = document.createElement("article");
    article.className = cards[0].className;
    const icon = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    icon.setAttribute("viewBox", "0 0 24 24");
    icon.setAttribute("class", "w-6 h-6 text-white");
    icon.setAttribute("fill", "none");
    icon.setAttribute("stroke", "currentColor");
    icon.setAttribute("stroke-width", "2");
    icon.setAttribute("aria-hidden", "true");
    const shape = document.createElementNS(icon.namespaceURI, "path");
    shape.setAttribute("d", path);
    icon.appendChild(shape);
    const title = document.createElement("h3");
    title.textContent = name;
    title.className = "font-bold text-sm mt-2";
    const copy = document.createElement("p");
    copy.textContent = description;
    copy.className = "text-xs text-slate-500 mt-1";
    const link = document.createElement("button");
    link.type = "button";
    link.setAttribute("aria-label", "Open " + name);
    link.textContent = "›";
    link.style.cssText = "text-align:right;color:#075cdb;font-size:20px";
    const iconBadge = cards[0].querySelector("[data-tool-icon]").cloneNode(false);
    iconBadge.appendChild(icon);
    [iconBadge, title, copy, link].forEach(child => article.appendChild(child));
    cards[0].parentElement.appendChild(article);
    article.dataset.category = category;
    article.dataset.route = route;
    cards.push(article);
  }
  const routes = ["agenda", "speakers", "name-tag", "venue"];
  const categories = ["Event", "Event", "Tools", "Event"];
  cards.forEach((card, index) => {
    const route = card.dataset.route || routes[index];
    const link = card.querySelector("button");
    link.addEventListener("click", () => open(route));
    card.addEventListener("click", event => {
      if (!event.target.closest("button")) open(route);
    });
  });
  const input = document.querySelector("input");
  const chips = all("button").filter(item => ["All", "Event", "Interactive", "Tools"].includes(text(item)));
  const filters = filterList(input, chips, cards,
    (category, index) => category === "All" || (cards[index].dataset.category || categories[index]) === category);
  bind("Grid Menu", () => { input.value = ""; filters.select("All"); });
}

function setupSpeakers() {
  const cards = all('[aria-label="Speaker directory"] article');
  const chips = all('[aria-label="Categories"] button');
  const saved = read("speakers", [true, false, false]);
  const categories = [["Nutrition", "Pediatrics"], ["Research"], ["Nutrition"]];
  let favoritesOnly = false;
  const filters = filterList(document.querySelector("input"), chips, cards, (category, index) =>
    (category === "All" || categories[index].includes(category)) && (!favoritesOnly || saved[index]));
  cards.forEach((card, index) => {
    const favorite = all("button", card)[0];
    function render() {
      favorite.classList.toggle("detail-saved", saved[index]);
      favorite.setAttribute("aria-pressed", String(Boolean(saved[index])));
      favorite.setAttribute("aria-label", saved[index] ? "Remove from favorites" : "Add to favorites");
      const icon = favorite.querySelector("svg");
      icon.setAttribute("fill", saved[index] ? "currentColor" : "none");
      if (!icon.hasAttribute("stroke")) icon.setAttribute("stroke", "currentColor");
    }
    render();
    favorite.addEventListener("click", () => {
      saved[index] = !saved[index];
      save("speakers", saved);
      render();
      filters.update();
    });
    all("button", card)[1].addEventListener("click", () => {
      const parts = all("h3, p", card).map(text);
      notify(parts.slice(0, 2).join(" · ") || "No additional speaker details are available.");
    });
  });
  bind("Favorites", () => {
    favoritesOnly = !favoritesOnly;
    button("Favorites").setAttribute("aria-pressed", String(favoritesOnly));
    filters.update();
  });
  bind("Open filter settings", () => chips[0].focus());
}

function setupResources() {
  const cards = all('[data-purpose="resources-list"] article');
  const saved = read("resources", [false, false, false]);
  const chips = all('[data-purpose="category-filters"] button');
  const formats = ["PDF", "PDF", "Slides"];
  const filters = filterList(document.querySelector("input"), chips, cards,
    (category, index) => category === "All" || (category === "Saved" ? saved[index] : formats[index] === category));
  cards.forEach((card, index) => {
    const bookmark = card.querySelector('[aria-label="Bookmark"]');
    function render() {
      bookmark.classList.toggle("detail-saved", saved[index]);
      bookmark.setAttribute("aria-pressed", String(Boolean(saved[index])));
      bookmark.setAttribute("aria-label", saved[index] ? "Remove bookmark" : "Bookmark");
      bookmark.querySelector("svg").setAttribute("fill", saved[index] ? "currentColor" : "none");
    }
    render();
    bookmark.addEventListener("click", () => {
      saved[index] = !saved[index];
      save("resources", saved);
      render();
      filters.update();
    });
    card.querySelector('[aria-label="Download"]').addEventListener("click", () => {
      notify("A download file hasn’t been provided for this resource.");
    });
  });
  bind("Open folder", () => filters.select("Saved"));
}

function setupVoting() {
  const polls = [
    ["How would you rate the current session?", ["Excellent", "Very Good", "Good"]],
    ["How useful was the session for your clinical practice?", ["Very useful", "Somewhat useful", "Not useful"]],
    ["Which topic would you like to see at the next congress?", ["Pediatric nutrition", "Clinical research", "Nutrition guidelines"]]
  ];
  const card = document.querySelector('[data-purpose="poll-content-card"]');
  const panel = card.firstElementChild;
  const heading = panel.querySelector("h2");
  const total = heading.nextElementSibling;
  const badge = panel.querySelector("[data-poll-status]");
  const dots = all("[data-poll-index]", panel);
  const options = all('[data-purpose^="poll-option-item"]', panel);
  const labels = options.map(option => option.querySelector(".font-bold"));
  const results = options.map(option => all(":scope > :not(:first-child), :scope > :first-child > :last-child", option));
  const circles = options.map(option => option.querySelector(".rounded-full"));
  const emptyClass = circles[0].className;
  const checkedClass = circles[1].className;
  const checkmark = circles[1].firstElementChild.cloneNode(true);
  const choices = read("poll-choices", [read("poll-choice", 1), null, null]);
  const [previous, next, nextArrow] = all('[data-purpose="bottom-controls"] button');
  let current = 0;
  let touchStart = null;
  let transition = null;

  options[1].classList.remove("border-2", "border-brand-primary", "bg-blue-50/20");
  options[1].classList.add("border", "border-slate-200", "bg-white");

  function render() {
    const [question, answers] = polls[current];
    heading.textContent = question;
    total.textContent = current === 0 ? "101 total votes" : "Your response is saved on this device.";
    badge.textContent = choices[current] == null ? "OPEN" : "VOTED";
    options.forEach((option, index) => {
      const selected = choices[current] === index;
      labels[index].textContent = answers[index];
      results[index].forEach(result => { result.hidden = current !== 0; });
      option.setAttribute("aria-label", answers[index]);
      option.setAttribute("aria-pressed", String(selected));
      option.classList.toggle("detail-selected", selected);
      circles[index].className = selected ? checkedClass : emptyClass;
      circles[index].textContent = "";
      if (selected) circles[index].appendChild(checkmark.cloneNode(true));
    });
    dots.forEach((dot, index) => dot.setAttribute("aria-pressed", String(index === current)));
    previous.disabled = current === 0;
    next.disabled = nextArrow.disabled = current === polls.length - 1;
  }

  function show(index) {
    if (index < 0 || index >= polls.length || index === current) return;
    const offset = index > current ? 20 : -20;
    current = index;
    render();
    if (panel.animate && !window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      if (transition) transition.cancel();
      transition = panel.animate([{ opacity: .35, transform: "translateX(" + offset + "px)" },
        { opacity: 1, transform: "translateX(0)" }], { duration: 190, easing: "ease-out" });
    }
  }

  options.forEach((option, index) => clickable(option, () => {
    choices[current] = index;
    save("poll-choices", choices);
    render();
    notify("Choice saved on this device.");
  }));
  dots.forEach((dot, index) => dot.addEventListener("click", () => show(index)));
  previous.addEventListener("click", () => show(current - 1));
  [next, nextArrow].forEach(control => control.addEventListener("click", () => show(current + 1)));
  bind("Voting Options", () => options[0].focus());

  document.body.addEventListener("touchstart", event => {
    touchStart = event.touches.length === 1 ? event.touches[0] : null;
  }, { passive: true });
  document.body.addEventListener("touchcancel", () => { touchStart = null; });
  document.body.addEventListener("touchend", event => {
    if (!touchStart) return;
    const end = event.changedTouches[0];
    const x = end.clientX - touchStart.clientX;
    const y = end.clientY - touchStart.clientY;
    touchStart = null;
    if (Math.abs(x) < 45 || Math.abs(x) <= Math.abs(y) * 1.3) return;
    if (event.cancelable) event.preventDefault();
    show(current + (x < 0 ? 1 : -1));
  }, { passive: false });
  render();
}


function setupQuestions() {
  const form = document.querySelector('[data-purpose="question-input-form"]');
  const input = form.querySelector("input");
  input.setAttribute("aria-label", "Write your question");
  const controls = all("button", form);
  const feed = document.querySelector('[data-purpose="questions-feed"]');
  const tabs = all('[data-purpose="filter-tabs"] button');
  const original = feed.querySelector("article");
  const drafts = read("questions", []);
  let topic = "General";
  let order = "Recent";
  function render() {
    all("[data-local-question]", feed).forEach(card => card.remove());
    const draft = order === "Recent" ? drafts[drafts.length - 1] : null;
    original.hidden = Boolean(draft);
    if (!draft) return;

    const card = document.createElement("article");
    card.className = original.className;
    card.dataset.localQuestion = "";
    const label = document.createElement("p");
    label.className = "text-xs font-semibold text-blue-700";
    label.textContent = draft.topic + " · Saved on this device";
    const question = document.createElement("p");
    question.className = "mt-2 text-sm text-slate-800";
    question.textContent = draft.question;
    card.appendChild(label);
    card.appendChild(question);
    feed.insertBefore(card, original);
  }

  chipGroup(controls.slice(0, -1), "General", value => { topic = value; });
  const selectOrder = chipGroup(tabs, "Recent", value => { order = value; render(); });
  function submit(event) {
    event.preventDefault();
    const question = input.value.trim();
    if (!question) { notify("Enter a question first."); input.focus(); return; }
    drafts.push({ topic, question });
    save("questions", drafts);
    input.value = "";
    input.blur();
    selectOrder("Recent");
    notify("Question saved on this device.");
  }
  controls[controls.length - 1].addEventListener("click", submit);
  input.addEventListener("keydown", event => { if (event.key === "Enter") submit(event); });
  bind("Open chat discussions", () => feed.scrollIntoView({ block: "start" }));
  bind("Upvote question", () => {
    const upvote = button("Upvote question");
    const selected = !read("question-upvote", false);
    save("question-upvote", selected);
    upvote.setAttribute("aria-pressed", String(selected));
    const count = upvote.querySelector("span");
    if (count) count.textContent = selected ? "25" : "24";
  });
  if (read("question-upvote", false)) {
    const upvote = button("Upvote question");
    if (upvote) upvote.setAttribute("aria-pressed", "true");
    const count = upvote && upvote.querySelector("span");
    if (count) count.textContent = "25";
  }
  all("button", feed).filter(item => text(item) === "Reply").forEach(reply => {
    reply.addEventListener("click", () => notify("Replies aren’t available for this demo question."));
  });
}

function setupAgenda() {
  const search = document.querySelector('[data-agenda-search]');
  const searchToggle = button("Search");
  const dayButtons = all("[data-agenda-day]");
  const cards = all("[data-agenda-category]");
  const chips = all("[data-agenda-filter] button");
  const empty = document.querySelector("[data-agenda-empty]");
  let day = "1";
  let category = "All";
  const saved = read("agenda-bookmarks", []);

  function update() {
    const query = normalize(search.value).replace(/\s+/g, " ");
    cards.forEach(card => {
      card.hidden = day !== card.dataset.day ||
        (category !== "All" && category !== card.dataset.agendaCategory) ||
        !normalize(text(card)).includes(query);
    });
    empty.textContent = day === "2" ? "The Day 2 schedule is not available yet." : "No matching sessions.";
    empty.hidden = cards.some(card => !card.hidden);
  }
  const selectCategory = chipGroup(chips, category, value => { category = value; update(); });

  dayButtons.forEach(tab => {
    tab.addEventListener("click", () => {
      day = tab.dataset.agendaDay;
      dayButtons.forEach(item => item.setAttribute("aria-pressed", String(item === tab)));
      document.querySelector("[data-agenda-heading]").textContent = "Day " + day + " Schedule";
      document.querySelector("[data-agenda-date]").textContent =
        day === "1" ? "Friday, May 22, 2026" : "Saturday, May 23, 2026";
      update();
    });
  });
  searchToggle.addEventListener("click", () => {
    search.hidden = !search.hidden;
    searchToggle.setAttribute("aria-expanded", String(!search.hidden));
    if (search.hidden) {
      search.value = "";
      update();
    } else {
      search.focus();
    }
  });
  search.addEventListener("input", update);
  document.querySelector("[data-agenda-reset]").addEventListener("click", () => {
    search.value = "";
    selectCategory("All");
  });

  cards.forEach(card => {
    const bookmark = card.querySelector("button");
    const title = text(card.querySelector("h4"));
    function render() {
      const selected = saved.includes(card.dataset.session);
      bookmark.setAttribute("aria-pressed", String(selected));
      bookmark.setAttribute("aria-label", (selected ? "Remove bookmark for " : "Bookmark ") + title);
      bookmark.querySelector("svg").setAttribute("fill", selected ? "currentColor" : "none");
    }
    bookmark.addEventListener("click", () => {
      const index = saved.indexOf(card.dataset.session);
      if (index === -1) saved.push(card.dataset.session);
      else saved.splice(index, 1);
      save("agenda-bookmarks", saved);
      render();
    });
    render();
  });
}

switch (screen) {
  case "agenda": setupAgenda(); break;
  case "quick-access": setupQuickAccess(); break;
  case "speakers": setupSpeakers(); break;
  case "resources": setupResources(); break;
  case "voting": setupVoting(); break;
  case "ask-qa": setupQuestions(); break;
  case "venue": bind("Open Navigation Directions", () => open("directions")); break;
  case "name-tag":
  case "name-tag-alternate": bind("Print Badge", () => open("print")); break;
  case "profile": {
    bind("Open Settings", () => notify("Profile settings aren’t available for the sample attendee."));
    bind("Edit Profile", () => notify("This screen shows the sample attendee profile."));
    break;
  }
}
document.documentElement.dataset.ready = "true";
