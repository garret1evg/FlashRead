# FlashReader GitHub Pages

Static site for FlashReader: home, Privacy Policy, Terms of Use, Support, and a 404 page.

```
pages/
├── index.html
├── privacy/index.html
├── terms/index.html
├── support/index.html
├── assets/styles.css
├── 404.html
├── .nojekyll
└── README.md
```

Copy the **contents** of this folder (not the `pages/` folder itself) to the root of a dedicated GitHub Pages repository. Paths are relative, so the site works at the domain root.

## Placeholders

Replace every placeholder below before you publish. Search the HTML files for the exact bracketed token (including spaces).

| Placeholder | Description | Used in |
|-------------|-------------|---------|
| `[DEVELOPER LEGAL NAME]` | Legal name of the developer or company that publishes FlashReader | `privacy/index.html` |
| `[PRIVACY CONTACT EMAIL]` | Email for privacy questions | `privacy/index.html` |
| `[SUPPORT EMAIL]` | Email for user support | `terms/index.html`, `support/index.html` |
| `[EFFECTIVE DATE]` | Date these documents first take effect (for example `1 September 2026`) | `privacy/index.html`, `terms/index.html` |
| `[LAST UPDATED DATE]` | Date of the latest revision | `privacy/index.html`, `terms/index.html` |
| `[FIREBASE RETENTION PERIOD]` | How long Google retains Firebase Analytics data for this project (from the Firebase / Google Analytics retention setting) | `privacy/index.html` |
| `[CHILDREN/TARGET AGE STATEMENT]` | Who the app is for and whether it is directed at children (must match Play Console target age and Data Safety) | `privacy/index.html` |
| `[GOVERNING LAW AND JURISDICTION]` | Applicable law and courts (for example the country or region where the developer is established) | `terms/index.html` |
| `[GOOGLE PLAY URL]` | Store listing URL once the app is on Google Play | `index.html` |

After replacement, no `[ALL CAPS ...]` tokens should remain. Check `mailto:` hrefs as well as visible text.

## Checklist before you publish

- [ ] Replace every placeholder in the table above.
- [ ] Confirm the Firebase SDK in the app matches what Privacy Policy describes (Analytics events, device/identifiers, diagnostics). Do not claim the app “does not collect data.”
- [ ] Confirm the YouTube transcript flow: a video URL may be sent to a network service; book and document files are not uploaded for that feature.
- [ ] Confirm the Firebase Analytics retention period in the Firebase / Google Analytics console and use that value for `[FIREBASE RETENTION PERIOD]`.
- [ ] Confirm the Play Console target age group and write `[CHILDREN/TARGET AGE STATEMENT]` to match it.
- [ ] Align this site with Play Console **Data safety**: local library vs technical data (Firebase, AdMob, UMP, Play Billing), and that uninstalling the app does not guarantee deletion from Google services.
- [ ] Confirm AdMob / UMP wording: consent, Privacy options in app settings, and advertising identifiers.
- [ ] Confirm Google Play Billing wording (payments processed by Google, purchase token) and refunds via Google Play.
- [ ] Check relative links (Home, Privacy, Terms, Support, CSS) from the site root and from `privacy/`, `terms/`, and `support/`.
- [ ] Confirm Google documentation links in the Privacy Policy still resolve (Firebase, AdMob, UMP, Play Terms).
- [ ] Keep `.nojekyll` in the repository root so GitHub Pages does not process the site with Jekyll.

`404.html` uses the same root-relative-to-file links as the home page (`assets/styles.css`, `index.html`, `privacy/index.html`). Those work when GitHub Pages serves the 404 file for a missing path at the site root. A missing nested path such as `/privacy/missing` will resolve those relative URLs one directory too deep. That is a GitHub Pages limitation of a static 404 file; use a custom domain or a user/organization site at the domain root if you want the 404 page to stay styled from every unknown URL.

## Deploy to GitHub Pages

1. Create a new public repository named `flashreader-pages` (or another name you prefer).
2. Copy the **contents** of this `pages/` folder into the repository root so `index.html` is at `/index.html`, not `/pages/index.html`.
3. Commit and push to the `main` branch.
4. In the repository: **Settings → Pages**.
5. Under **Build and deployment**:
   - Source: **Deploy from a branch**
   - Branch: **main**
   - Folder: **/ (root)**
6. Save and wait for the Pages build to finish.
7. Open the generated URL (typically `https://<user-or-org>.github.io/flashreader-pages/` if the repo is not a user/organization site).

If the site is served from a project-pages path (`/flashreader-pages/`) instead of a custom domain or `*.github.io` user site, relative links in this folder still work as long as you copied files to the repo root. Do not nest them under an extra `pages/` directory on the live site.

The included `.nojekyll` file tells GitHub Pages to skip Jekyll. Keep it at the site root.

## Custom domain (optional)

You can point a domain you already own at this site. Exact DNS records depend on your registrar and whether you use an apex domain or a subdomain; follow GitHub’s current custom-domain documentation.

Typical steps:

1. In the `flashreader-pages` repository, add a `CNAME` file at the site root. The file should contain only your hostname, one line, no `https://` (for example `www.example.com`).
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

If you use a project-pages URL, include the repository path (`https://<user>.github.io/flashreader-pages/privacy/`).
