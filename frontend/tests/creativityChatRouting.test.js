import test from 'node:test';
import assert from 'node:assert/strict';
import {routeCreativityChat, moodboardFixtureEnabled} from '../src/lib/creativityChatRouting.js';
import {moodboardPreviewFixture} from './fixtures/moodboardPreviewFixture.js';

const cases = [
  ['SANA, create a dark fantasy moodboard for my game.', 'moodboard'],
  ['Please generate a mood board with cold colors.', 'moodboard'],
  ['Can you make a moodboard for my story?', 'moodboard'],
  ['صممي لوحة إلهام للعبة غامضة', 'moodboard'],
  ['سنا، أنشئي لوحة الإلهام لقريتي المهجورة', 'moodboard'],
  ['سوي مود بورد بألوان باردة', 'moodboard'],
  ['Create a moodboard and change my story.', 'clarify'],
  ['Create a moodboard and a character.', 'clarify'],
  ['Create a character and a moodboard.', 'clarify'],
  ['أنشئ لوحة إلهام وعدل القصة', 'clarify'],
  ['أنشئ لوحة إلهام وشخصية', 'clarify'],
  ['Dark fantasy moodboard', 'clarify'],
  ['لوحة إلهام غامضة', 'clarify'],
  ['Update my moodboard', 'clarify'],
  ['What makes a good moodboard?', 'existing'],
  ['Explain my moodboard.', 'existing'],
  ['How can I create a moodboard?', 'existing'],
  ['Give me advice about my moodboard.', 'existing'],
  ["Don't create a moodboard. Explain my story.", 'existing'],
  ['كيف أصمم لوحة إلهام؟', 'existing'],
  ['اشرح لوحة الإلهام', 'existing'],
  ['لا تنشئ لوحة إلهام', 'existing'],
  ['Create a character inspired by my moodboard.', 'existing'],
  ['Create a story map using the moodboard.', 'existing'],
  ['أنشئ شخصية مستوحاة من لوحة الإلهام', 'existing'],
  ['Create a new story map', 'existing'],
  ['Edit this character', 'existing'],
  ['Suggest a plot twist', 'existing'],
  ['   ', 'existing']
];

for (const domain of ['writing', 'games']) {
  for (const [text, expected] of cases) {
    test(`${domain}: ${text}`, () => {
      assert.equal(routeCreativityChat({domain, text}), expected);
    });
  }
}

for (const domain of ['technology', 'business', 'education', 'TECH', 'BUSINESS', 'EDUCATION', undefined]) {
  test(`${domain}: moodboard language and explicit mode never intercept other domains`, () => {
    for (const mode of ['auto', 'moodboard']) {
      assert.equal(routeCreativityChat({domain, text: 'Create a moodboard', mode}), 'existing');
    }
  });
}

test('explicit in-chat moodboard destination handles otherwise unrecognized wording', () => {
  assert.equal(routeCreativityChat({domain: 'games', text: 'Cold forest references', mode: 'moodboard'}), 'moodboard');
  assert.equal(routeCreativityChat({domain: 'games', text: ' ', mode: 'moodboard'}), 'existing');
});

test('backend Creativity domain name is supported', () => {
  assert.equal(routeCreativityChat({domain: 'CREATIVITY', text: 'Create a moodboard'}), 'moodboard');
});

test('fixture requires both development mode and explicit URL opt-in', () => {
  assert.equal(moodboardFixtureEnabled(true, '?moodboardPreview=1'), true);
  assert.equal(moodboardFixtureEnabled(false, '?moodboardPreview=1'), false);
  assert.equal(moodboardFixtureEnabled(undefined, '?moodboardPreview=1'), false);
  assert.equal(moodboardFixtureEnabled(true, ''), false);
  assert.equal(moodboardFixtureEnabled(true, '?moodboardPreview=0'), false);
});

for (const lang of ['ar', 'en']) {
  test(`${lang}: fixture contains no image addresses, Openverse IDs, or save identity`, () => {
    const fixture = moodboardPreviewFixture(lang);
    assert.equal(fixture.kind, 'development-fixture');
    assert.ok(fixture.placeholders.length > 0);
    assert.ok(fixture.notes.length > 0);
    assert.ok(fixture.palette.every(color => /^#[0-9A-F]{6}$/.test(color.hex)));
    assert.doesNotMatch(JSON.stringify(fixture), /https?:|data:|openverseId|imageUrl|boardId|outputId/);
  });
}
