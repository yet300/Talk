# Prompt for Claude Design, pass 2

Paste everything below the line into Claude Design on the same canvas
(https://claude.ai/artifact/DfFymLRByEHfUcREbTDFjX), as the reply to its pass-1 report.

---

Thank you, this is a strong pass. Answers to your questions and notes, one product change, and real data for
the videos. Change only what is listed here; keep the rest as it is.

## 1. Product change: phone navigation

On **Phone**, the bottom capsule becomes **Home, Search, Activity, Videos** (four items):

- **Videos** replaces Bookmarks as a tab (play icon). Remove the Videos header button you added to the phone
  Home; Home keeps Following · Popular · Latest.
- **Bookmarks moves into your own profile**: your profile's tabs become **Posts · Replies · Bookmarks**
  (Bookmarks only on your own profile, never on someone else's). The bookmarks list, its empty state ("Nothing
  saved yet", "Bookmark a post and it stays here, ready to read later.", Back to Home) and the remove action
  stay the same, now under that tab.
- Your profile still opens from the avatar in the Home header; Settings from its gear.
- **Web** keeps its rail (Home, Search, Activity, Videos, Bookmarks, Profile, Settings) and **Duo** keeps its
  side column as you drew them; this change is for the Phone size class only. Update the Phone artboards
  (Timeline, Profile (yours), Bookmarks → becomes the profile tab, Videos home, every board that shows the
  capsule) and the notes.

## 2. Answers to your "Problems with the decisions"

1. **Editing kind 1 posts:** agreed. Add one line to the editor in edit mode, near Save: "Edits show in Talk.
   Other Nostr apps keep showing the original." Also show "Edited" next to the time on cards and in the
   reader for edited posts (it opens Revisions).
2. **`nsec` in the browser:** agreed; the code side adds a strict Content-Security-Policy and keeps markdown
   sanitized, and signers stay on the roadmap. Nothing to draw beyond point 3.
3. **Backup:** yes, add both:
   - After **Create a new key**, a one-time "Save your key" step: the `nsec` in a mono box, Copy, "I saved it"
     (primary, enabled after Copy or after 3 seconds), and the warning that it can't be recovered.
   - **Sign out** confirm dialog: "Sign out?", "Without your private key you can't sign back in to this
     account.", a secondary **Show private key** that reveals and lets you copy it inside the dialog, then the
     red Sign out and Cancel.
4. **YouTube oEmbed:** it does work from the browser. We tested
   `https://www.youtube.com/oembed?url=…&format=json` with an `Origin` header and YouTube returns
   `access-control-allow-origin` for it, and thumbnails load from `i.ytimg.com`. Keep the rich YouTube card for
   any YouTube link, not only catalog videos.
5. **Reports are public:** agreed. The Report sheet says it before you choose: "Reports are public on Nostr:
   the person you report can see that you reported them." The confirmation becomes "Reported. Your report is
   public." Keep the categories.

## 3. Your other notes

- **Beyond the brief** (Search-Web, your own profile, More-Actions, Compose-Contexts, Activity-Empty,
  Videos-Empty, and the copy you wrote): accepted, keep them.
- **GitHub icon:** use the official GitHub mark (Octicons `mark-github`, MIT licensed) at the same 24×24 size,
  filled in currentColor; GitHub's brand rules allow it to link a GitHub profile.
- **Duo Search:** draw Search for Duo outer (single pane) and Duo inner sideways (results | the opened post or
  profile) instead of linking to the phone board.
- **Duo inner Profile tab:** it should open your profile, not the editor; the editor opens from "Edit profile".
- **Filters:** build Level and Type as menus as your note says; for topics see the data below.
- **Inert controls:** fine, the notes are what we build from.

## 4. Real video data (replace the `[Sample]` content)

The catalog is a download, not the pages: `https://hackertube.org/download/videos.jsonl` (one JSON object per
line, ODC-By 1.0). 6,454 videos. Filters use the **150 categories** (`categories[].name`), not
`primary_topic` (that one is free text with ~5,600 values; show it as a small label). `content_type` is one of
course, lecture, talk, tutorial, explainer, documentary, interview, podcast, demo, commentary, news, music,
comedy, other; `level` is intro, intermediate, advanced or general. Thumbnails:
`https://i.ytimg.com/vi/<vid>/hqdefault.jpg` (`mqdefault.jpg` for small cards). Use these six as the sample set:

| vid | title | channel | type · level | category | why HN | HN quote | recommenders |
|---|---|---|---|---|---|---|---|
| VMj-3S1tku0 | The spelled-out intro to neural networks and backpropagation: building micrograd | Andrej Karpathy | lecture · intro | Machine learning & deep learning | HN treats it as the best starting point for really understanding backprop and neural nets. | "he explains the why and provides intuitive explanations" | 17 |
| fNk_zzaMoSs | Vectors \| Chapter 1, Essence of linear algebra | 3Blue1Brown | course · intro | Pure mathematics | HN's go-to recommendation for learning linear algebra intuition. | "the most welcoming and informative introduction to the topic" | 17 |
| Unzc731iCUY | How to Speak | MIT OpenCourseWare | lecture · general | University lectures & courses | The canonical, near-universally recommended lecture on giving talks. | "drop what you're doing and watch it now" | 17 |
| NGFhc8R_uO4 | Indistinguishable From Magic: Manufacturing Modern Computer Chips | Andor Gafotas | talk · general | Semiconductors & chip manufacturing | The best in-depth introduction to IC manufacturing. | "the most in depth explanation of IC manufacuring I have seen" | 19 |
| FITJMJjASUs | Ruby Conf 12 - Y Not- Adventures in Functional Programming by Jim Weirich | Confreaks | talk · intermediate | Functional programming | One of the best and most entertaining programming talks. | "the most entertaining and interesting technical talk" | 17 |
| aMcJ1Jvtef0 | 8 Bit & '8 Bitish' Graphics-Outside the Box | GDC Festival of Gaming | talk · intermediate | Game history & retro gaming | A master class in limited-palette pixel art and palette cycling. | "anyone who is interested in the details and history" | 13 |

Summaries for the video page (the first one; write the others in the same length from the titles):
"Andrej Karpathy's first Neural Networks: Zero to Hero lecture, which builds micrograd, a tiny autograd engine,
from scratch in Python. It explains backpropagation and neural network training step by step."

## Deliverables of this pass

- Phone navigation and the profile Bookmarks tab (section 1), with notes.
- Editor edit-mode line and "Edited" on cards and the reader (2.1).
- "Save your key" step after Create a new key, and the new Sign out dialog (2.3).
- Report sheet copy (2.5); GitHub mark; Duo Search boards; Duo inner Profile tab fix.
- Real catalog content and thumbnails in Videos home, the video page and video cards in posts.
- Light sheets for anything new.

As before: one note per new or changed board with "Actions: …" for every control.
