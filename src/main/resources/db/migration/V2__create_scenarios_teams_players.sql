-- SCENARIOS
CREATE TABLE scenarios (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id    UUID        NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    name          VARCHAR(100) NOT NULL,
    current_phase VARCHAR(30) NOT NULL DEFAULT 'OFFSEASON',
    current_year  INT         NOT NULL DEFAULT 1,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_scenario_name_per_account UNIQUE (account_id, name)
);

-- TEAMS
CREATE TABLE teams (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    scenario_id UUID        NOT NULL REFERENCES scenarios(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    budget      BIGINT      NOT NULL DEFAULT 0,
    is_user_team BOOLEAN    NOT NULL DEFAULT false,
    CONSTRAINT uq_team_name_per_scenario UNIQUE (scenario_id, name)
);

-- PLAYERS
CREATE TABLE players (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    scenario_id          UUID        NOT NULL REFERENCES scenarios(id) ON DELETE CASCADE,
    first_name           VARCHAR(50) NOT NULL,
    last_name            VARCHAR(50) NOT NULL,
    position             VARCHAR(10) NOT NULL,    -- GUARD | FORWARD
    height_inches        INT         NOT NULL,
    age                  INT         NOT NULL DEFAULT 18,
    potential            INT         NOT NULL,    -- 1-7
    potential_remaining  INT         NOT NULL,    -- decrements each season
    growth               INT         NOT NULL,    -- 3-8
    longevity            INT         NOT NULL,    -- 1-7
    longevity_remaining  INT         NOT NULL,    -- decrements after potential exhausted
    decay                INT         NOT NULL,    -- 5-10
    current_team_id      UUID        REFERENCES teams(id) ON DELETE SET NULL,
    original_team_id     UUID        REFERENCES teams(id) ON DELETE SET NULL,
    is_benched           BOOLEAN     NOT NULL DEFAULT false,
    is_free_agent        BOOLEAN     NOT NULL DEFAULT false
);

-- PLAYER SKILLS
CREATE TABLE player_skills (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id   UUID        NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    skill_type  VARCHAR(20) NOT NULL,   -- FOUR_POINT | TWO_POINT | FREE_THROW | PASSING | STEALING | BLOCKING | REBOUNDING
    value       INT         NOT NULL DEFAULT 1,
    CONSTRAINT uq_player_skill UNIQUE (player_id, skill_type)
);

-- PLAYER SEASON STATS
CREATE TABLE player_season_stats (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id            UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    scenario_id          UUID NOT NULL REFERENCES scenarios(id) ON DELETE CASCADE,
    team_id              UUID REFERENCES teams(id) ON DELETE SET NULL,
    year_number          INT  NOT NULL,
    games_played         INT  NOT NULL DEFAULT 0,
    two_point_attempts   INT  NOT NULL DEFAULT 0,
    two_point_made       INT  NOT NULL DEFAULT 0,
    four_point_attempts  INT  NOT NULL DEFAULT 0,
    four_point_made      INT  NOT NULL DEFAULT 0,
    free_throw_attempts  INT  NOT NULL DEFAULT 0,
    free_throw_made      INT  NOT NULL DEFAULT 0,
    passes_attempted     INT  NOT NULL DEFAULT 0,
    passes_completed     INT  NOT NULL DEFAULT 0,
    steals_attempted     INT  NOT NULL DEFAULT 0,
    steals_made          INT  NOT NULL DEFAULT 0,
    blocks_attempted     INT  NOT NULL DEFAULT 0,
    blocks_made          INT  NOT NULL DEFAULT 0,
    rebounds_attempted   INT  NOT NULL DEFAULT 0,
    rebounds_made        INT  NOT NULL DEFAULT 0,
    total_points         INT  NOT NULL DEFAULT 0,
    possession_wins      INT  NOT NULL DEFAULT 0,
    possession_losses    INT  NOT NULL DEFAULT 0,
    CONSTRAINT uq_player_season UNIQUE (player_id, scenario_id, year_number, team_id)
);

