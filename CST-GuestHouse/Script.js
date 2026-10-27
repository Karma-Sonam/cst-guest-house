(function () {
  /* ---------------- Auth (demo email + OTP login) ---------------- */
  const Auth = {
    SESSION_KEY: "gh_session",
    OTP_KEY: "gh_otp",
    OTP_TTL: 10 * 60 * 1000, // 10 minutes

    session() {
      try {
        return JSON.parse(localStorage.getItem(this.SESSION_KEY));
      } catch {
        return null;
      }
    },
    isLoggedIn() {
      return !!this.session();
    },
    login(email) {
      localStorage.setItem(
        this.SESSION_KEY,
        JSON.stringify({ email, ts: Date.now() }),
      );
    },
    logout() {
      localStorage.removeItem(this.SESSION_KEY);
    },
    requestOtp(email) {
      const otp = String(Math.floor(100000 + Math.random() * 900000));
      localStorage.setItem(
        this.OTP_KEY,
        JSON.stringify({ email, otp, ts: Date.now() }),
      );
      /* TODO: replace this with a real email send, e.g.:
         fetch('/api/send-otp', {
           method: 'POST',
           headers: { 'Content-Type': 'application/json' },
           body: JSON.stringify({ email, otp })
         });
         or integrate EmailJS / SendGrid / your backend here.
      */
      console.info(`[Guest House demo] OTP for ${email}: ${otp}`);
      return otp;
    },
    verifyOtp(email, code) {
      let rec;
      try {
        rec = JSON.parse(localStorage.getItem(this.OTP_KEY));
      } catch {
        rec = null;
      }
      if (!rec) return false;
      const fresh = Date.now() - rec.ts < this.OTP_TTL;
      return fresh && rec.email === email && rec.otp === code;
    },
  };
  window.Auth = Auth;

  /* ---------------- Header / Footer ---------------- */
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
    const session = Auth.session();
    const acctHtml = session
      ? `<span class="muted" style="font-size:.82rem">${session.email}</span><button class="btn ghost" id="logout-link" type="button">Log out</button>`
      : `<a class="btn" href="login.html">Login</a>`;

    /* Top info bar (logo image + contact details) sits above the sticky nav.
       The brand badge now displays an actual logo image instead of a
       text/emoji placeholder. */
    header.innerHTML = `
      <div class="top-bar">
        <div class="container top-bar-in">
          <a class="brand-badge" href="index.html" aria-label="Guest House home">
            <span class="brand-badge-ring">
              <img src="Images/logo.jpg" alt="Guest House logo" />
            </span>
            <span class="brand-badge-text">Guest House</span>
          </a>
          <ul class="top-info">
            <li>
              <span class="ti-icon">📍</span>
              <span class="ti-text"><b>Location</b><span>Awaiting property address</span></span>
            </li>
            <li>
              <span class="ti-icon">☎</span>
              <span class="ti-text"><b>Call Us</b><span>Awaiting phone number</span></span>
            </li>
            <li>
              <span class="ti-icon">✉</span>
              <span class="ti-text"><b>Email</b><span>Awaiting email address</span></span>
            </li>
          </ul>
        </div>
      </div>
      <nav class="nav" aria-label="Main">
        <div class="nav-in">
          ${nav}
          <button class="menu-btn" aria-label="Menu" aria-expanded="false">☰</button>
          <form id="search" role="search"><input type="search" placeholder="Search" aria-label="Search the site"></form>
          <div class="acct">${acctHtml}</div>
        </div>
      </nav>`;

    const n = header.querySelector(".nav"),
      navBar = header.querySelector(".nav"),
      b = header.querySelector(".menu-btn");
    b.onclick = () => {
      const o = n.classList.toggle("open");
      b.setAttribute("aria-expanded", o);
    };
    const logoutLink = header.querySelector("#logout-link");
    if (logoutLink)
      logoutLink.onclick = () => {
        Auth.logout();
        location.href = "index.html";
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
        book: "rooms.html",
        enquir: "contact.html",
        contact: "contact.html",
        phone: "contact.html",
        email: "contact.html",
        login: "login.html",
        account: "login.html",
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
    window.addEventListener(
      "scroll",
      () => navBar.classList.toggle("scrolled", window.scrollY > 10),
      { passive: true },
    );

    /* Lock icon on booking links when logged out (rooms page) */
    if (!Auth.isLoggedIn()) {
      document.querySelectorAll('a[href^="booking.html"]').forEach((a) => {
        if (!a.querySelector(".lock"))
          a.insertAdjacentHTML(
            "beforeend",
            ' <span class="lock" title="Login required">🔒</span>',
          );
      });
    }
  }
  const footer = document.getElementById("site-footer");
  if (footer) {
    footer.innerHTML = `<div class="container"><div class="foot"><div><div class="foot-brand"><img src="Images/logo.jpg" alt="Guest House logo" /><b>Guest House</b></div><p style="font-size:.88rem;margin-top:6px;max-width:300px">A refined guest house experience with thoughtful stays, welcoming spaces, and the comfort of a home away.</p></div>
<div><b>Quick links</b><ul>${pages.map(([h, t]) => `<li><a href="${h}">${t}</a></li>`).join("")}</ul></div>
<div><b>Contact</b><ul><li><a href="contact.html">Email</a></li><li><a href="contact.html">Phone</a></li><li><a href="contact.html">Address</a></li></ul></div></div>
<div class="copy"><span>© ${new Date().getFullYear()} Guest House. All rights reserved.</span><span><a href="#" aria-label="Instagram">Instagram</a> · <a href="#" aria-label="Facebook">Facebook</a> · <a href="#" aria-label="Twitter">Twitter</a></span></div></div>`;
  }

  /* ---------------- Scroll-reveal animation ---------------- */
  function setupReveal() {
    const els = document.querySelectorAll(
      ".card, .room, .sec-head, .photo, .review, .floor, .auth-card, .auth-art",
    );
    els.forEach((el, i) => {
      el.classList.add("reveal-auto");
      el.style.transitionDelay = (i % 6) * 0.06 + "s";
    });
    if (!("IntersectionObserver" in window)) {
      els.forEach((el) => el.classList.add("in-view"));
      return;
    }
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add("in-view");
            io.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.12 },
    );
    els.forEach((el) => io.observe(el));
  }
  setupReveal();

  /* ---------------- Booking page auth-gate ---------------- */
  const bookingUserEl = document.getElementById("booking-user");
  if (bookingUserEl) {
    const params = new URLSearchParams(location.search);
    const room = params.get("room") || "";
    if (!Auth.isLoggedIn()) {
      location.href =
        "login.html?redirect=booking.html" +
        (room ? "&room=" + encodeURIComponent(room) : "");
    } else {
      const session = Auth.session();
      document
        .querySelectorAll("#booking-user, #side-user")
        .forEach((el) => (el.textContent = session.email));
      document.body.classList.remove("gate");
      const logoutBtn = document.getElementById("logout-btn");
      if (logoutBtn)
        logoutBtn.onclick = () => {
          Auth.logout();
          location.href = "index.html";
        };
      const f = document.getElementById("enquiry-form");
      if (f && f.email) {
        f.email.value = session.email;
        f.email.readOnly = true;
      }
    }
  }

  /* ---------------- Login page logic ---------------- */
  const emailForm = document.getElementById("email-form");
  if (emailForm) {
    const otpForm = document.getElementById("otp-form");
    const stepEmail = document.getElementById("step-email");
    const stepOtp = document.getElementById("step-otp");
    const otpEmailLabel = document.getElementById("otp-email");
    const demoBox = document.getElementById("demo-otp");
    const statusEl = document.getElementById("auth-status");
    const params = new URLSearchParams(location.search);
    const redirect = params.get("redirect") || "index.html";
    const room = params.get("room");
    const dest = redirect + (room ? "?room=" + encodeURIComponent(room) : "");
    let currentEmail = "";

    if (Auth.isLoggedIn()) location.href = dest;

    emailForm.addEventListener("submit", (e) => {
      e.preventDefault();
      const email = emailForm.email.value.trim();
      const errEl = emailForm.querySelector('[data-err="email"]');
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        errEl.textContent = "Please enter a valid email address.";
        emailForm.email.classList.add("invalid");
        return;
      }
      errEl.textContent = "";
      emailForm.email.classList.remove("invalid");
      const btn = emailForm.querySelector("button");
      const spin = btn.querySelector(".spin");
      spin.hidden = false;
      btn.disabled = true;
      setTimeout(() => {
        const otp = Auth.requestOtp(email);
        currentEmail = email;
        otpEmailLabel.textContent = email;
        demoBox.textContent = `Demo mode (no email server connected): your code is ${otp}.`;
        stepEmail.hidden = true;
        stepOtp.hidden = false;
        spin.hidden = true;
        btn.disabled = false;
        otpForm.otp.focus();
      }, 500);
    });

    otpForm.addEventListener("submit", (e) => {
      e.preventDefault();
      const code = otpForm.otp.value.trim();
      const errEl = otpForm.querySelector('[data-err="otp"]');
      if (Auth.verifyOtp(currentEmail, code)) {
        errEl.textContent = "";
        otpForm.otp.classList.remove("invalid");
        Auth.login(currentEmail);
        statusEl.textContent = "Signed in! Redirecting…";
        statusEl.classList.add("show");
        setTimeout(() => (location.href = dest), 500);
      } else {
        errEl.textContent = "Incorrect or expired code. Try again or resend.";
        otpForm.otp.classList.add("invalid");
      }
    });

    document.getElementById("resend-otp").onclick = () => {
      if (!currentEmail) return;
      const otp = Auth.requestOtp(currentEmail);
      demoBox.textContent = `Demo mode: your new code is ${otp}.`;
    };
    document.getElementById("change-email").onclick = () => {
      stepOtp.hidden = true;
      stepEmail.hidden = false;
    };
  }

  /* ---------------- Enquiry / Booking form ---------------- */
  const f = document.getElementById("enquiry-form");
  if (f) {
    const today = new Date().toISOString().split("T")[0];
    f.arrival.min = today;
    f.departure.min = today;
    f.arrival.addEventListener("change", () => {
      f.departure.min = f.arrival.value || today;
    });
    const room = new URLSearchParams(location.search).get("room");
    if (room && f.room) f.room.value = room;
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
        "Thank you! Your booking request was received (preview only – delivery is not configured yet). This does not confirm a reservation.";
      s.classList.add("show");
      const keepEmail = f.email.value;
      const wasReadonly = f.email.readOnly;
      f.reset();
      if (wasReadonly) {
        f.email.value = keepEmail;
        f.email.readOnly = true;
      }
    });
    f.addEventListener("blur", (e) => check(e.target), true);
  }
})();
