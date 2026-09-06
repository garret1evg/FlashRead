# FlashReader GitHub Pages

Static site for FlashReader: home, Privacy Policy, Terms of Use, Support, and a 404 page.

```
docs/
├── index.html
├── privacy/index.html
├── terms/index.html
├── support/index.html
├── assets/styles.css
├── 404.html
├── .nojekyll
└── README.md
```

This folder is the GitHub Pages site for the FlashRead repository. GitHub Pages can publish `/docs` from `main`. Paths are relative to this folder, so they work when the published site root is `docs/`.

## Published values

Keep these identical in the HTML here and in `LegalDocuments.kt`.

| Fact | Value |
|------|-------|
| Developer legal name | Yevhen Chmutov |
| Privacy and support email | `yevhen.chmutov.support@gmail.com` |
| Effective date | 31 August 2026 |
| Last updated | 6 September 2026 (in-app: `September 6, 2026`) |
| Firebase Analytics retention | 14 months |
| Firebase Crashlytics retention | 90 days |
| Children | FlashReader is not directed at children under 13. We do not knowingly collect personal information from children. |
| Governing law | the laws of Poland and the courts of Poland |
| Google Play URL | Not published yet. Home page says the app will be available on Google Play. Add the store URL when the listing exists. |

## Checklist before you publish

- [x] Fill developer name, contact email, dates, Firebase retention, children statement, and governing law. Leave the Play Store URL until the listing exists.
- [ ] Confirm the Firebase SDK in the app matches what Privacy Policy describes (Analytics events, Crashlytics reports, device/identifiers, diagnostics). Do not claim the app “does not collect data.”
- [x] Confirm the Firebase Analytics retention period in the Firebase / Google Analytics console (14 months).
- [x] Confirm Firebase Crashlytics retention (90 days).
- [x] Confirm the target age statement: FlashReader is not directed at children under 13.
- [ ] Align this site with Play Console **Data safety**: local library vs technical data (Firebase Analytics, Crashlytics, AdMob, UMP, Play Billing), and that uninstalling the app does not guarantee deletion from Google services.
- [ ] Confirm AdMob / UMP wording: consent, Privacy options in app settings, and advertising identifiers.
- [ ] Confirm Google Play Billing wording (payments processed by Google, purchase token) and refunds via Google Play.
- [ ] Check relative links (Home, Privacy, Terms, Support, CSS) from the site root and from `privacy/`, `terms/`, and `support/`.
- [ ] Confirm Google documentation links in the Privacy Policy still resolve (Firebase, AdMob, UMP, Play Terms).
- [ ] Keep `.nojekyll` in this `docs/` folder so GitHub Pages does not process the site with Jekyll.

`404.html` uses the same root-relative-to-file links as the home page (`assets/styles.css`, `index.html`, `privacy/index.html`). Those work when GitHub Pages serves the 404 file for a missing path at the site root. A missing nested path such as `/privacy/missing` will resolve those relative URLs one directory too deep. That is a GitHub Pages limitation of a static 404 file; use a custom domain or a user/organization site at the domain root if you want the 404 page to stay styled from every unknown URL.

## Deploy to GitHub Pages

1. Commit and push this `docs/` folder to the `main` branch of the FlashRead repository. The repository must be public for free GitHub Pages.
2. In the repository: **Settings → Pages**.
3. Under **Build and deployment**:
   - Source: **Deploy from a branch**
   - Branch: **main**
   - Folder: **/docs**
4. Save and wait for the Pages build to finish.
5. Open the generated URL (typically `https://<user-or-org>.github.io/FlashRead/`).

Relative links in this folder still work when the site is served from that project-pages path. Do not nest the HTML under an extra `docs/` or `pages/` directory on the live site — GitHub already publishes this folder as the site root.

The included `.nojekyll` file tells GitHub Pages to skip Jekyll. Keep it in `docs/`.

## Custom domain (optional)

You can point a domain you already own at this site. Exact DNS records depend on your registrar and whether you use an apex domain or a subdomain; follow GitHub’s current custom-domain documentation.

Typical steps:

1. In this `docs/` folder, add a `CNAME` file. The file should contain only your hostname, one line, no `https://` (for example `www.example.com`).
2. In **Settings → Pages**, enter the same hostname under **Custom domain** and save.
3. At your DNS provider, add the records GitHub shows for that hostname (apex and/or subdomain).
4. Wait for DNS to propagate, then enable HTTPS in the Pages settings once the certificate is issued.

Do not commit guessed DNS values. Use the records GitHub displays for your account.

## URLs for Play Console and the app

After the site is live (replace the host with your Pages or custom domain):

| Use | Path |
|-----|------|
| Home | `/` or `/index.html` |
| Privacy Policy | `/privacy/` or `/privacy/index.html` |
| Terms of Use | `/terms/` or `/terms/index.html` |
| Support | `/support/` or `/support/index.html` |

Play Console:

- **App content → Privacy policy**: paste the Privacy Policy URL (for example `https://<your-domain>/privacy/`).
- Add the same Privacy Policy URL in the app where Play policy requires a link (for example settings or the about screen).
- Optionally link Terms and Support from the store listing or in-app help.

If you use a project-pages URL, include the repository path (`https://<user>.github.io/FlashRead/privacy/`).
