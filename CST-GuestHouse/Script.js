(function () {
  const pages = [
    ["index.html", "Home"],
    ["rooms.html", "Rooms"],
    ["contact.html", "Contact"],
    ["about.html", "About us"],
  ];
  const here = (
    location.pathname.split("/").pop() || "index.html"
  ).toLowerCase();
  const nav = pages
    .map(
      ([h, t]) =>
        `<a class="link" href="${h}"${h === here ? ' aria-current="page"' : ""}>${t}</a>`,
    )
    .join("");
  const header = document.getElementById("site-header");
  if (header) {
    header.innerHTML = `<nav class="nav" aria-label="Main"><a class="logo" href="index.html" aria-label="Guest House home">⌂</a>${nav}
<button class="menu-btn" aria-label="Menu" aria-expanded="false">☰</button>
<form id="search" role="search"><input type="search" placeholder="Search" aria-label="Search the site"></form></nav>`;
    const n = header.querySelector(".nav"),
      b = header.querySelector(".menu-btn");
    b.onclick = () => {
      const o = n.classList.toggle("open");
      b.setAttribute("aria-expanded", o);
    };
    header.querySelector("#search").onsubmit = (e) => {
      e.preventDefault();
      const q = e.target.querySelector("input").value.trim().toLowerCase();
      if (!q) return;
      const map = {
        room: "rooms.html",
        suite: "rooms.html",
        garden: "rooms.html",
        family: "rooms.html",
        book: "contact.html",
        enquir: "contact.html",
        contact: "contact.html",
        phone: "contact.html",
        email: "contact.html",
        about: "about.html",
        welcome: "about.html",
        philosoph: "about.html",
        home: "index.html",
        map: "index.html",
        review: "index.html",
      };
      const k = Object.keys(map).find((k) => q.includes(k));
      if (k) location.href = map[k];
      else
        e.target.querySelector("input").placeholder = 'No match – try "rooms"';
      e.target.reset();
    };
  }
  const footer = document.getElementById("site-footer");
  if (footer) {
    footer.innerHTML = `<div class="container"><div class="foot"><div><b>⌂ Guest House</b><p style="font-size:.88rem;margin-top:6px;max-width:300px">A refined guest house experience with thoughtful stays, welcoming spaces, and the comfort of a home away.</p></div>
<div><b>Quick links</b><ul>${pages.map(([h, t]) => `<li><a href="${h}">${t}</a></li>`).join("")}</ul></div>
<div><b>Contact</b><ul><li><a href="contact.html">Email</a></li><li><a href="contact.html">Phone</a></li><li><a href="contact.html">Address</a></li></ul></div></div>
<div class="copy"><span>© ${new Date().getFullYear()} Guest House. All rights reserved.</span><span><a href="#" aria-label="Instagram">Instagram</a> · <a href="#" aria-label="Facebook">Facebook</a> · <a href="#" aria-label="Twitter">Twitter</a></span></div></div>`;
  }
  /* enquiry form */
  const f = document.getElementById("enquiry-form");
  if (f) {
    const today = new Date().toISOString().split("T")[0];
    f.arrival.min = today;
    f.departure.min = today;
    f.arrival.addEventListener("change", () => {
      f.departure.min = f.arrival.value || today;
    });
    const room = new URLSearchParams(location.search).get("room");
    if (room) f.room.value = room;
    document
      .querySelectorAll("[data-room]")
      .forEach((c) => (c.onclick = () => (f.room.value = c.dataset.room)));
    const rules = {
      name: (v) => (v.trim() ? "" : "Please enter your name."),
      email: (v) =>
        /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v)
          ? ""
          : "Please enter a valid email address.",
      message: (v) =>
        v.trim().length >= 5 ? "" : "Please tell us about your stay.",
      consent: (v, el) => (el.checked ? "" : "Please agree to be contacted."),
    };
    const check = (el) => {
      const r = rules[el.name];
      if (!r) return true;
      const m = r(el.value, el);
      el.classList.toggle("invalid", !!m);
      const e = f.querySelector(`[data-err="${el.name}"]`);
      if (e) e.textContent = m;
      return !m;
    };
    f.addEventListener("submit", (e) => {
      e.preventDefault();
      let ok = [...f.elements].map(check).every(Boolean);
      if (
        f.arrival.value &&
        f.departure.value &&
        f.departure.value <= f.arrival.value
      ) {
        ok = false;
        f.querySelector('[data-err="departure"]').textContent =
          "Departure must be after arrival.";
      } else f.querySelector('[data-err="departure"]').textContent = "";
      const s = document.getElementById("form-status");
      if (!ok) {
        s.classList.remove("show");
        f.querySelector(".invalid")?.focus();
        return;
      }
      /* TODO: send data to your backend / Formspree / EmailJS here */
      console.log(Object.fromEntries(new FormData(f)));
      s.textContent =
        "Thank you! Your enquiry was received (preview only – delivery is not configured yet). This does not confirm a booking.";
      s.classList.add("show");
      f.reset();
    });
    f.addEventListener("blur", (e) => check(e.target), true);
  }
})();
