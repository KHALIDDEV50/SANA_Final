<p align="center">
  <img src="frontend/public/sana-logo.png" alt="SANA logo" width="320">
</p>

# SANA

SANA (سنا) is a web platform that turns a rough idea into a structured, visual project. Users describe an idea, and SANA helps them shape it into diagrams and workspaces for **Technology, Business, Education and Creativity** (writing and games), with an AI assistant that proposes, edits and reviews the work.

## Problem

People with a new idea usually don't know where to start: which kind of plan or diagram fits it, how to structure it, or which tool to use. They end up switching between separate tools for system design, business analysis, study notes and story planning, and generic AI chats give unstructured text that has to be copied and redrawn by hand.

## Solution

One workspace that structures any idea and keeps the AI's output usable:

- **Start with SANA AI** – write an idea; the AI picks the right domain and diagram, then draws the first version directly onto the canvas.
- **Domain diagrams** on a shared canvas:
  - **Technology:** system architecture, ERD, UML class diagram, use-case diagram.
  - **Business:** business model canvas, target audience, SWOT, competitor analysis, risk analysis, plus structured AI reports.
  - **Education:** mind maps.
  - **Creativity:** story maps (characters, worlds, locations, rules, events), branching game narratives with variables and play-through preview, character balance, moodboards, book inspiration, Yarn export.
- **AI assistant** that creates, edits or advises on a diagram. Every change arrives as a **proposal** the user accepts or rejects, and is checked against the diagram's rules before it is saved.
- **Accounts and plans** – HTTP Basic login, each user sees only their own projects, and optional paid plans (Moyasar, test mode) raise the project limit.

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java , Spring Boot, Spring Data JPA (Hibernate), Spring Security (HTTP Basic), Bean Validation, Lombok |
| Database | MySQL 8.4 (deployed on AWS RDS) |
| Frontend | React, Vite |
| AI | n8n Cloud workflows; *Sana - AI Gateway* and *Sana - AI Quick Start* |
| Payments | Moyasar |
| External data | Open Library (books); Cleveland Museum of Art, The Met, Wikimedia Commons and Pixabay; World Bank (Saudi indicators) |
| Deployment | AWS EC2 + RDS, Docker, Docker Compose |

## API endpoints

**Access rules.** Every endpoint requires HTTP Basic login (email + password) unless marked **Public**. Endpoints that take a project, diagram or element ID check that it belongs to the logged-in user (admins may access everything). Endpoints marked **Admin** are for the `ADMIN` role only.

### Authentication

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/v1/auth/register` | **Public.** Create an account (role `USER`; the password is hashed) |
| GET | `/api/v1/auth/me` | The logged-in user; used by the frontend to check the login |
| POST | `/api/v1/auth/logout` | End the session |

### Projects

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/project/get` | **Admin.** All projects |
| GET | `/api/v1/project/get/my` | The logged-in user's projects |
| GET | `/api/v1/project/get/{projectId}` | One project |
| GET | `/api/v1/project/get/user/{userId}` | A user's projects |
| POST | `/api/v1/project/add/{userId}` | Create a project (checks the plan's project limit when billing is on) |
| PUT | `/api/v1/project/update/{projectId}` | Update a project |
| DELETE | `/api/v1/project/delete/{projectId}` | Delete a project |

### Diagrams (all domains)

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/diagram/types` | Supported diagram types with their node and edge types and rules |
| GET | `/api/v1/diagram/get/project/{projectId}` | A project's diagrams (optionally filtered by type) |
| GET | `/api/v1/diagram/get/{outputId}` | One diagram |
| GET | `/api/v1/diagram/graph/get/{outputId}` | A diagram's full graph and current revision |
| POST | `/api/v1/diagram/add/{projectId}` | Create an empty diagram of a given type |
| PUT | `/api/v1/diagram/update/{outputId}` | Rename a diagram |
| DELETE | `/api/v1/diagram/delete/{outputId}` | Delete a diagram |
| GET | `/api/v1/diagram/node/get/{outputId}/{nodeId}` | One node |
| POST | `/api/v1/diagram/node/add/{outputId}` | Add a node |
| PUT | `/api/v1/diagram/node/update/{outputId}/{nodeId}` | Update a node |
| DELETE | `/api/v1/diagram/node/delete/{outputId}/{nodeId}` | Delete a node and its edges |
| GET | `/api/v1/diagram/edge/get/{outputId}/{edgeId}` | One edge |
| POST | `/api/v1/diagram/edge/add/{outputId}` | Add an edge |
| PUT | `/api/v1/diagram/edge/update/{outputId}/{edgeId}` | Update an edge |
| DELETE | `/api/v1/diagram/edge/delete/{outputId}/{edgeId}` | Delete an edge |

### AI assistant

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/v1/ai/quick-start/classify` | Quick start: the AI suggests a domain, diagram type and project name for an idea |
| POST | `/api/projects/{projectId}/ai/requests` | Start a CREATE, EDIT or ADVISE request (answered later through n8n) |
| GET | `/api/projects/{projectId}/ai/requests` | Latest 50 requests; `?messages=true` also returns prompts and answers (chat history) |
| GET | `/api/projects/{projectId}/ai/requests/{requestId}` | Poll a request's status and result |
| GET | `/api/projects/{projectId}/ai/proposals/{proposalId}` | Preview a proposed diagram and its changes |
| POST | `/api/projects/{projectId}/ai/proposals/{proposalId}/accept` | Apply a proposal to the diagram |
| POST | `/api/projects/{projectId}/ai/proposals/{proposalId}/reject` | Reject a proposal |
| POST | `/api/projects/{projectId}/creativity/ai/requests` | Creativity AI request (with conversation history) |
| POST | `/api/integrations/n8n/ai/callback` | **Public** (secured by the `X-Sana-Callback-Key` header). n8n delivers an AI result |

### Business

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/business/tools` | Run a Business tool and get a structured AI report. One endpoint for all five tools, chosen by `toolType`: `BUSINESS_MODEL_BUILDER`, `TARGET_AUDIENCE_BUILDER`, `SWOT_ANALYZER`, `COMPETITOR_ANALYSIS`, `RISK_ANALYZER` |
| GET | `/api/v1/external/world-bank/saudi-indicators` | Saudi economic indicators from the World Bank (standalone; not yet fed into the AI reports) |

Business diagrams (business model canvas, target audience, SWOT, competitor analysis, risk analysis) are created and edited through the shared [Diagrams](#diagrams-all-domains) endpoints, and their AI requests and proposals go through the shared [AI assistant](#ai-assistant) endpoints.

### Technology – architecture diagrams

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/tech/architecture/get` | **Admin.** All architecture diagrams |
| GET | `/api/v1/tech/architecture/get/{outputId}` | One architecture diagram |
| GET | `/api/v1/tech/architecture/get/project/{projectId}` | A project's architecture diagrams |
| GET | `/api/v1/tech/architecture/graph/get/{outputId}` | Full architecture graph |
| POST | `/api/v1/tech/architecture/add/{projectId}` | Create an architecture diagram |
| PUT | `/api/v1/tech/architecture/update/{outputId}` | Update it |
| DELETE | `/api/v1/tech/architecture/delete/{outputId}` | Delete it |
| GET / POST / PUT / DELETE | `/api/v1/tech/architecture/node/{get\|add\|update\|delete}/{outputId}[/{nodeId}]` | Manage its components (nodes) |
| GET / POST / PUT / DELETE | `/api/v1/tech/architecture/edge/{get\|add\|update\|delete}/{outputId}[/{edgeId}]` | Manage its connections (edges) |

### Creativity – story elements

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/character-profile/get` | **Admin.** All characters |
| GET | `/api/v1/character-profile/get/by/{id}` | One character |
| POST | `/api/v1/character-profile/add/{outputId}` | Add a character to a story map |
| PUT | `/api/v1/character-profile/update/{id}` | Update a character |
| DELETE | `/api/v1/character-profile/delete/{id}` | Delete a character |
| GET | `/api/v1/world/get` | **Admin.** All worlds |
| GET | `/api/v1/world/get/{id}` | One world |
| POST | `/api/v1/world/add/{outputId}` | Add a world |
| PUT | `/api/v1/world/update/{id}` | Update a world |
| DELETE | `/api/v1/world/delete/{id}` | Delete a world |
| GET | `/api/v1/location/get` | **Admin.** All locations |
| GET | `/api/v1/location/get/{id}` | One location |
| POST | `/api/v1/location/add/{outputId}` | Add a standalone location |
| POST | `/api/v1/location/add/{outputId}/world/{worldId}` | Add a location inside a world |
| POST | `/api/v1/location/add/{outputId}/parent/{parentLocationId}` | Add a location inside another location |
| POST | `/api/v1/location/add/{outputId}/world/{worldId}/parent/{parentLocationId}` | Add a location with both a world and a parent |
| PUT | `/api/v1/location/update/{id}` | Update a location and its position |
| PUT | `/api/v1/location/update/{locationId}/world/{worldId}` | Move a location to another world |
| PUT | `/api/v1/location/update/{locationId}/parent/{parentLocationId}` | Change a location's parent (no cycles) |
| DELETE | `/api/v1/location/delete/{id}` | Delete a location (not if it has child locations) |
| GET | `/api/v1/world-rule/get` | **Admin.** All world rules |
| GET | `/api/v1/world-rule/get/{id}` | One world rule |
| POST | `/api/v1/world-rule/add/{outputId}` | Add an independent world rule |
| POST | `/api/v1/world-rule/add/{outputId}/world/{worldId}` | Add a rule to a world |
| PUT | `/api/v1/world-rule/update/{id}` | Update a world rule |
| PUT | `/api/v1/world-rule/update/{ruleId}/world/{worldId}` | Assign a rule to another world |
| DELETE | `/api/v1/world-rule/delete/{id}` | Delete a world rule |
| GET | `/api/v1/story-event/get` | **Admin.** All story events |
| GET | `/api/v1/story-event/get/{id}` | One story event |
| POST | `/api/v1/story-event/add/{outputId}` | Add a story event |
| PUT | `/api/v1/story-event/update/{id}` | Update a story event |
| DELETE | `/api/v1/story-event/delete/{id}` | Delete a story event |
| GET | `/api/v1/story-event/timeline/presentation/{outputId}` | Timeline in narrative order |
| GET | `/api/v1/story-event/timeline/chronological/{outputId}` | Timeline in chronological order |
| GET | `/api/v1/creativity/relations/get/{outputId}` | All relationships in a story map |
| GET | `/api/v1/creativity/relations/get/{outputId}/node/{nodeId}` | Relationships of one element |
| GET | `/api/v1/creativity/relations/get/types/{outputId}/{sourceNodeId}/{targetNodeId}` | Relationship types allowed between two elements |
| POST | `/api/v1/creativity/relations/add/{outputId}/{sourceNodeId}/{targetNodeId}` | Create a relationship |
| GET | `/api/v1/creativity/relations/delete/preview/{outputId}/node/{nodeId}` | What deleting an element would affect |
| GET | `/api/v1/creativity/search/{outputId}` | Search the elements of a story map |

### Creativity – branching narrative (games)

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/branching/get/{outputId}/map` | Full branching map (scenes, positions, choices) |
| GET | `/api/v1/branching/get/{outputId}/nodes` | All scenes |
| GET | `/api/v1/branching/get/{outputId}/nodes/{nodeId}` | One scene |
| POST | `/api/v1/branching/add/{outputId}/nodes` | Add a scene (DIALOGUE, DECISION, EVENT, ENDING) |
| PUT | `/api/v1/branching/update/{outputId}/nodes/{nodeId}` | Update a scene |
| DELETE | `/api/v1/branching/delete/{outputId}/nodes/{nodeId}` | Delete a scene (not if it has choices) |
| PUT | `/api/v1/branching/update/{outputId}/nodes/{nodeId}/position` | Move a scene on the canvas |
| PUT | `/api/v1/branching/update/{outputId}/layout/nodes/{nodeIds}` | Move several scenes at once |
| POST | `/api/v1/branching/add/{outputId}/choices/{sourceNodeId}/{targetNodeId}` | Connect two scenes with a player choice |
| PUT | `/api/v1/branching/update/{outputId}/choices/{choiceId}/to/{targetNodeId}` | Change a choice's destination |
| DELETE | `/api/v1/branching/delete/{outputId}/choices/{choiceId}` | Delete a choice |
| GET | `/api/v1/branching/get/{outputId}/choices/{choiceId}/settings` | A choice's condition and effect |
| GET | `/api/v1/branching/get/{outputId}/variables` | Game variables |
| POST | `/api/v1/branching/add/{outputId}/variables` | Add a variable |
| PUT | `/api/v1/branching/update/{outputId}/variables/{variableId}` | Update a variable |
| DELETE | `/api/v1/branching/delete/{outputId}/variables/{variableId}` | Delete an unused variable |
| PUT | `/api/v1/branching/set/{outputId}/choices/{choiceId}/condition/{variableId}` | Set a choice's condition |
| DELETE | `/api/v1/branching/remove/{outputId}/choices/{choiceId}/condition` | Remove a choice's condition |
| PUT | `/api/v1/branching/set/{outputId}/choices/{choiceId}/effect/{variableId}` | Set a choice's effect |
| DELETE | `/api/v1/branching/remove/{outputId}/choices/{choiceId}/effect` | Remove a choice's effect |
| PUT | `/api/v1/branching/link/{outputId}/nodes/{nodeId}/speaker/{characterId}` | Link a character as a scene's speaker |
| DELETE | `/api/v1/branching/unlink/{outputId}/nodes/{nodeId}/speaker` | Remove the speaker |
| PUT | `/api/v1/branching/link/{outputId}/nodes/{nodeId}/event/{eventId}` | Link a story event to a scene |
| DELETE | `/api/v1/branching/unlink/{outputId}/nodes/{nodeId}/event` | Remove the event link |
| GET | `/api/v1/branching/get/{outputId}/nodes/{nodeId}/references` | A scene's linked character and event |
| GET | `/api/v1/branching/play/{outputId}/start/{nodeId}` | Start a play-through preview |
| POST | `/api/v1/branching/play/{outputId}/{nodeId}/choose/{choiceId}` | Make a choice in the preview |
| GET | `/api/v1/branching/export/yarn/{outputId}` | Export as Yarn (from the first scene) |
| GET | `/api/v1/branching/export/yarn/{outputId}/start/{startNodeId}` | Export as Yarn from a chosen scene |

### Creativity – character balance (games)

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/character-balance/get/{outputId}/characters` | Playable characters |
| GET | `/api/v1/character-balance/{outputId}/characters/{characterId}` | One playable character and its stats |
| POST | `/api/v1/character-balance/{outputId}/characters/add` | Add a playable character |
| PUT | `/api/v1/character-balance/{outputId}/characters/update/{characterId}` | Update a playable character |
| DELETE | `/api/v1/character-balance/{outputId}/characters/delete/{characterId}` | Delete a playable character |
| PUT | `/api/v1/character-balance/{outputId}/characters/{characterId}/profile/{profileId}` | Link it to a story character |
| DELETE | `/api/v1/character-balance/{outputId}/characters/{characterId}/profile` | Unlink the story character |
| GET | `/api/v1/character-balance/{outputId}/analyze` | Analyze the balance of the characters' stats |

### Creativity – AI assist tools

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/projects/{projectId}/creativity/assist/balance` | AI suggests character stat changes (draft) |
| POST | `/api/projects/{projectId}/creativity/assist/books` | AI suggests real books for inspiration (draft) |
| GET | `/api/projects/{projectId}/creativity/assist/drafts/{draftId}` | Poll an assist draft |
| POST | `/api/projects/{projectId}/creativity/assist/drafts/{draftId}/approve-balance` | Apply the approved stat changes |

### Creativity – moodboards

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/creativity/moodboard/images/search` | Search images (Cleveland Museum, The Met, Wikimedia, and Pixabay when its key is set) |
| GET | `/api/v1/creativity/moodboard/images/pixabay-assets/{id}` | Serve a saved Pixabay image |
| GET | `/api/v1/creativity/moodboard/images/{openverseId}` | Image attribution details (legacy) |
| POST | `/api/v1/creativity/moodboard/add/{projectId}` | Create a moodboard |
| GET | `/api/v1/creativity/moodboard/get/project/{projectId}` | A project's moodboards |
| GET | `/api/v1/creativity/moodboard/get/{outputId}` | One moodboard |
| PUT | `/api/v1/creativity/moodboard/update/{outputId}/title` | Rename a moodboard |
| POST | `/api/v1/creativity/moodboard/add/{outputId}/images` | Add an image |
| POST | `/api/v1/creativity/moodboard/add/{outputId}/labels` | Add a text label |
| PUT | `/api/v1/creativity/moodboard/update/{outputId}/items/{itemId}/position` | Move an image or label |
| PUT | `/api/v1/creativity/moodboard/update/{outputId}/labels/{itemId}` | Edit a label |
| DELETE | `/api/v1/creativity/moodboard/delete/{outputId}/items/{itemId}` | Remove an item |
| GET | `/api/projects/{projectId}/creativity/moodboard-ai/capabilities` | Whether AI moodboards are available |
| POST | `/api/projects/{projectId}/creativity/moodboard-ai/drafts` | AI generates a moodboard preview |
| GET | `/api/projects/{projectId}/creativity/moodboard-ai/drafts/{draftId}` | Poll the preview |
| POST | `/api/projects/{projectId}/creativity/moodboard-ai/drafts/{draftId}/approve` | Save the preview as a moodboard |
| POST | `/api/projects/{projectId}/creativity/moodboard-ai/drafts/{draftId}/reject` | Discard the preview |
| GET | `/api/v1/creativity/books/search` | Search Open Library for books |
| GET | `/api/v1/creativity/books/get/{workId}` | Book details |

### Billing (Moyasar, test mode)

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/billing/plans` | Plans, prices and project limits |
| GET | `/api/billing/me` | The user's current plan, usage and period end |
| GET | `/api/billing/orders` | The user's last 10 payment orders |
| POST | `/api/billing/checkout` | Start paying for a plan; returns the Moyasar checkout URL |
| POST | `/api/billing/reconcile` | Re-check the user's payments with Moyasar |
| POST | `/api/billing/callbacks/{orderId}/{token}` | **Public** (per-order secret token). Moyasar payment notification |

### Project context and low-level graph data

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/project-understanding/get` | **Admin.** All project understandings |
| GET | `/api/v1/project-understanding/get/{id}` | One project understanding |
| GET | `/api/v1/project-understanding/get/project/{projectId}` | A project's goal, actors, components and constraints (AI context) |
| POST | `/api/v1/project-understanding/add/{projectId}` | Add it |
| PUT | `/api/v1/project-understanding/update/{id}` | Update it |
| DELETE | `/api/v1/project-understanding/delete/{id}` | Delete it |
| GET | `/api/v1/project-output/get` | **Admin.** All project outputs |
| GET | `/api/v1/project-output/get/{outputId}` | One output (diagram, moodboard, …) |
| GET | `/api/v1/project-output/get/project/{projectId}` | A project's outputs |
| POST | `/api/v1/project-output/add/{projectId}` | Add an output |
| PUT | `/api/v1/project-output/update/{outputId}` | Update an output |
| DELETE | `/api/v1/project-output/delete/{outputId}` | Delete an output |
| GET | `/api/v1/node/get` | **Admin.** All nodes |
| POST | `/api/v1/node/add/{outputId}` | Add a node to an output |
| PUT | `/api/v1/node/update/{projectNodeId}` | Update a node |
| DELETE | `/api/v1/node/delete/{nodeId}` | Delete a node |
| GET | `/api/v1/edge/get` | **Admin.** All edges |
| POST | `/api/v1/edge/add/{outputId}/{sourceNodeId}/{targetNodeId}` | Connect two nodes |
| PUT | `/api/v1/edge/update/{projectEdgeId}` | Update an edge |
| DELETE | `/api/v1/edge/delete/{edgeId}` | Delete an edge |
| GET | `/api/v1/record/get` | **Admin.** All project records |
| POST | `/api/v1/record/add/{projectId}` | Add a project record |
| PUT | `/api/v1/record/update/{projectRecordId}` | Update a project record |
| DELETE | `/api/v1/record/delete/{projectRecordId}` | Delete a project record |

### Administration (Admin only)

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/user/get` | All users |
| POST | `/api/v1/user/add` | Create a user |
| PUT | `/api/v1/user/update/{userId}` | Update a user |
| DELETE | `/api/v1/user/delete/{userId}` | Delete a user |
| GET / POST / PUT / DELETE | `/api/v1/ai-interaction/{get\|add\|update\|delete}/…` | Raw AI request records |
| GET / POST / PUT / DELETE | `/api/v1/change-proposal/{get\|add\|update\|delete}/…` | Raw AI proposals |
| GET / POST / PUT / DELETE | `/api/v1/change-proposal-item/{get\|add\|update\|delete}/…` | Raw proposal changes |
| GET / POST / PUT / DELETE | `/api/v1/tool-run/{get\|add\|update\|delete}/…` | Raw tool-run records |
