# Vector — Decisions log

Why things are the way they are. Append new entries at the top. One decision per block.
Keeps future-you (and Claude) from re-litigating settled calls.

---

## No "Co-Authored-By: Claude" trailer on commits
History was scrubbed of the trailer via `git filter-branch` + force-push. Do not re-add it.
User does not want Claude listed as a contributor.

## Package name stays `com.example.fitness_tracker`
Not renamed despite the "Vector" rebrand — kept for Firebase compatibility.

## No DI / no tests / no Hilt
Repository is a singleton via `companion object get(context)`. Deliberate for a solo portfolio app; revisit only if the app grows collaborators.

## No real blur (Haze) for glass surfaces
Translucent Material 3 surfaces show "white rectangles" when content scrolls behind in light mode. Real fix was the Haze GPU-blur dependency; user tried it and reverted — wants the glass look but not the extra dependency. Accept the tradeoff.

## FloatingNavBar paints over screen content
Nav bar is a top-level sibling drawn after the NavHost, fully opaque. Screens must reserve bottom padding (`NAV_BAR_HEIGHT_DP + 24 + sysBottom`) so content clears it — there is no in-screen z-index that lifts content above the bar.

## Dark mode is pitch black (#000000)
AMOLED-friendly. Light surfaces are `#FCFCFC`.

## Brand violet only on primary CTAs + nav selection pill
`#5B3FFF` light / `#9B89FF` dark. `primaryContainer` neutralized to gray so violet doesn't leak into selected surfaces. Each tab has its own accent (Plan teal, Diet amber, Stats steel blue, Streak coral).
