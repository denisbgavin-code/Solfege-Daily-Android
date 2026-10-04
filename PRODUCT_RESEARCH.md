# Product research and design rationale — Quest v4

## Why v3 was not good enough

The previous prototype had several structural problems, not cosmetic ones:

1. **One-tap pseudo-games.** Some tasks effectively revealed the answer while the child was constructing it.
2. **Weak answer space.** Too many tasks had no plausible distractors, so success did not require discrimination.
3. **Feedback without diagnosis.** Green/red state existed, but the feedback often did not tell the child what musical feature to attend to.
4. **Progression fragility.** Completing a mission did not reliably advance the next-day state.
5. **UI looked like a form.** The child saw text cards and ordinary buttons rather than a coherent game world with musical actions.
6. **Gamification was mostly points/progress.** These elements can motivate, but they do not by themselves create learning.

Quest v4 replaces those assumptions rather than decorating them.

## What was borrowed from strong existing products

### EarMaster

Useful patterns:
- thousands of progressive exercises;
- voice input and pitch detection;
- rhythm tapping;
- multiple response modalities;
- detailed progress tracking;
- supplemental questions when a minimum score is not reached;
- adaptive learning / targeted practice;
- contextual help.

Official sources:
- https://www.earmaster.com/
- https://www.earmaster.com/products/ear-training-sight-singing/earmaster-for-mobile.html
- https://www.earmaster.com/support/earmaster-cloud/guides-for-students.html

What Quest v4 takes:
- multimodal response;
- several questions per skill instead of one decorative interaction;
- adaptive return to weak skills;
- local pitch coach;
- progress by musical competence.

What Quest v4 deliberately changes:
- no adult-professional density in the child interface;
- no requirement to reach a numerical pass mark before the child can leave a task; difficulty is adapted later instead of creating a dead end.

### Complete Ear Trainer / Complete Rhythm Trainer / Complete Music Reading Trainer

Useful patterns:
- progressive chapters and many drills;
- multiple drill types rather than one mechanic repeated everywhere;
- random generation of practice material;
- detailed result screens;
- arcade/game framing;
- custom programs and teacher use;
- rhythm imitation, reading, dictation and construction as separate skills.

Official source:
- https://binaryguilt.com/
- https://binaryguilt.com/blog/category/complete-ear-trainer/

What Quest v4 takes:
- progressive drill architecture;
- genuinely different task types;
- generated variants so repeated practice is not memorization of one item;
- dictation and construction, not only recognition.

### ToneSavvy / Tenuto family of ideas

Useful patterns:
- identification and construction are separate task families;
- rhythm dictation requires building the heard rhythm;
- melodic dictation is a constructive answer;
- functional scale-degree tasks establish tonal context before asking for the degree/interval.

Official sources:
- https://tonesavvy.com/music-practice-exercises/
- https://www.musictheory.net/exercises

What Quest v4 takes:
- separate recognition vs construction;
- explicit Check action in constructive tasks;
- tonal context for interval work;
- plausible near-neighbour distractors.

### Meludia

Useful patterns:
- listening is treated as multidimensional rather than just interval naming;
- graded auditory work across melody, form, rhythm, spatialization and harmony;
- large quantity of short progressive exercises.

Official source:
- https://meludia.com/en/what-is-meludia/

What Quest v4 takes:
- short repeated listening decisions;
- progression from contour and stability toward interval/key discrimination;
- variation in the perceptual dimension being trained.

### Yousician

Useful patterns:
- clear personal learning path;
- immediate feedback while doing musical actions;
- progress visualization;
- rewards and levels;
- daily practice framing.

Official source:
- https://yousician.com/

What Quest v4 takes:
- visible journey and daily mission;
- immediate process feedback;
- concise task framing.

What Quest v4 does not copy:
- performance scoring is not treated as the whole pedagogy;
- no public leaderboard;
- no loss-of-streak punishment;
- no attempt to turn every action into a high-score race.

## Evidence constraints

### Gamification is not enough

A 2024 meta-analysis found a small average positive effect of gamification on intrinsic motivation, with stronger effects on perceived autonomy/relatedness than competence. A separate 2024 systematic review of primary education found no solid general evidence that gamification itself improves learning outcomes.

Therefore Quest v4 treats game elements as **interface and motivation support**, while the learning engine is based on:
- discriminative listening;
- constructive responses;
- explanation after errors;
- repeated retrieval;
- adaptive difficulty;
- transfer/creation.

Sources:
- Li, Hew & Du (2024), Educational Technology Research and Development. DOI: 10.1007/s11423-023-10337-7
- Romero-Rodríguez et al. (2024), International Journal of Educational Research. DOI: 10.1016/j.ijer.2024.102481

### Feedback must say what to do next

A 2024 meta-analysis of digitally delivered instructional feedback reported an overall positive effect on learning and found that feedback focus and learner control matter. Another meta-analysis of technology-rich learning environments found explanation feedback stronger than simple correctness feedback.

Quest v4 therefore:
- does not stop at red/green;
- after the first error points attention to the relevant musical feature;
- after repeated error reveals the answer with an explanation;
- records supported success differently from independent success.

Sources:
- Brummer et al. (2024), Learning Environments Research. DOI: 10.1007/s10984-024-09501-4
- feedback meta-analysis, Learning and Instruction / technology-rich environments, 2023.

### Retrieval and spacing

Retrieval practice has consistent classroom evidence across many studies. Spacing is promising, but optimal schedules are not universal across domains.

Quest v4 uses:
- daily curriculum target;
- return to previously weak skills;
- generated variants rather than exact repetition.

Sources:
- Agarwal, Nunes & Blunt (2021), Educational Psychology Review. DOI: 10.1007/s10648-021-09595-9
- Carpenter, Pan & Butler (2022), Nature Reviews Psychology. DOI: 10.1038/s44159-022-00089-1
- Bego et al. (2024), International Journal of STEM Education. DOI: 10.1186/s40594-024-00468-5

### Interleaving

Interleaving can improve delayed discrimination/strategy choice, but can make practice feel harder and effects depend on prior knowledge and domain.

Quest v4 therefore does not randomly mix everything from day one. New material is first scaffolded; later missions and smart training interleave already-learned skills.

Relevant evidence:
- Carter & Grahn (2016), Frontiers in Psychology. DOI: 10.3389/fpsyg.2016.01251
- Klimovich et al. (2025), Learning and Instruction. DOI: 10.1016/j.learninstruc.2025.102146

### Self-determination: competence and autonomy

Game mechanics are designed to support:
- **competence:** clear challenge, useful feedback, retry, visible mastery;
- **autonomy:** no lives, no punishment for error, choice to replay, optional microphone;
- **relatedness:** story world and adult/teacher interpretation can be added without public comparison.

Sources:
- Wang et al. (2024), Learning and Motivation. DOI: 10.1016/j.lmot.2024.102015
- Li, Hew & Du (2024), DOI above.

## Core game loop in v4

Each daily mission has six stages:

1. **Ear detective** — 4 questions with plausible distractors; typically 3 independent correct answers earns full stars.
2. **Rhythm reconnaissance** — identify an actually heard pattern among altered alternatives.
3. **Note navigator** — connect heard pitch with note naming/reading.
4. **Constructive task** — melodic or rhythm dictation. The answer remains editable and is checked only on explicit submission.
5. **Voice bridge** — listening → singing; optional local pitch helper.
6. **Your music** — transfer/creation under one explicit musical constraint.

Errors never make the route impossible. They reduce the mastery estimate and schedule better support later.

## Progression invariant

The progression logic is intentionally simple and testable:

- completed lesson N → unlock N+1;
- mission completion is committed once, at the final mission screen;
- stars and mastery are separate from unlock state;
- wrong answers can lower mastery but cannot corrupt progression;
- unit tests explicitly verify day 1 → day 2 → day 3 unlocking.

## Product claim limits

Quest v4 is evidence-informed; it is not yet an evidence-validated intervention. A serious claim that it improves first-year solfeggio learning more than conventional practice would require a prospective study with children, a comparison condition, pre/post outcomes and preferably delayed retention measures.
