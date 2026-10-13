/* Green Force - shared login session for every page.
 * - keeps the JWT in localStorage
 * - turns the navbar "Login" link into "My Issues | Logout (name)" when logged in
 * - sends the token automatically with jQuery AJAX calls (Report.html), so a report
 *   made while logged in is linked to the account
 */
(function () {
  var TK = 'gf_token', UK = 'gf_user';

  function read(k) { try { return localStorage.getItem(k); } catch (e) { return null; } }

  var GF = window.GF = {
    getToken: function () { return read(TK); },
    getUser: function () { try { return JSON.parse(read(UK) || 'null'); } catch (e) { return null; } },
    setSession: function (d) {
      try {
        localStorage.setItem(TK, d.token);
        localStorage.setItem(UK, JSON.stringify({ name: d.name, email: d.email, role: d.role }));
      } catch (e) { /* storage blocked */ }
    },
    clear: function () { try { localStorage.removeItem(TK); localStorage.removeItem(UK); } catch (e) {} },

    /** JSON fetch helper. Resolves with parsed body, rejects with Error(message from server). */
    api: function (url, opts) {
      opts = opts || {};
      opts.headers = Object.assign({ 'Content-Type': 'application/json' }, opts.headers || {});
      var t = GF.getToken();
      if (t) opts.headers['Authorization'] = 'Bearer ' + t;
      return fetch(url, opts).then(function (r) {
        if (r.status === 204) return null;
        return r.json().catch(function () { return {}; }).then(function (d) {
          if (!r.ok) {
            if (r.status === 401 && t) GF.clear();          // expired / invalid token
            throw new Error(d.message || ('Error ' + r.status));
          }
          return d;
        });
      });
    }
  };

  if (window.jQuery && window.jQuery.ajaxPrefilter) {
    window.jQuery.ajaxPrefilter(function (options, original, jqXHR) {
      var t = GF.getToken();
      if (t && !options.crossDomain) jqXHR.setRequestHeader('Authorization', 'Bearer ' + t);
    });
  }

  // The original Admin.html only shows hard-coded sample data; send everyone to the live admin page.
  if (/\/Admin\.html$/i.test(window.location.pathname)) {
    window.location.replace('admin-panel.html');
    return;
  }

  function updateNavbar() {
    var user = GF.getUser();
    var links = document.querySelectorAll('.nav-menu a.nav-item');
    for (var i = 0; i < links.length; i++) {
      var a = links[i];
      var href = a.getAttribute('href');
      if (href === 'Admin.html') {
        a.setAttribute('href', 'admin-panel.html');          // the live admin page
      } else if (href === 'Login.html' && !user) {
        a.removeAttribute('onclick');                       // drop the "modal would open here" placeholder alert
      } else if (href === 'Login.html' && user) {
        var mine = document.createElement('a');
        mine.className = 'nav-item';
        mine.href = 'my-issues.html';
        mine.textContent = 'My Issues';
        a.parentNode.insertBefore(mine, a);

        a.removeAttribute('onclick');
        a.textContent = 'Logout (' + ((user.name || '').split(' ')[0] || 'me') + ')';
        a.setAttribute('href', '#');
        a.addEventListener('click', function (e) {
          e.preventDefault();
          GF.clear();
          window.location.href = 'home.html';
        });
      }
    }
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', updateNavbar);
  else updateNavbar();
})();
