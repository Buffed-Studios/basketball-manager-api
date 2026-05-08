-- GAMES TABLE
CREATE TABLE games (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    scenario_id   UUID        NOT NULL REFERENCES scenarios(id) ON DELETE CASCADE,
    home_team_id  UUID        NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    away_team_id  UUID        NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    year_number   INT         NOT NULL,
    game_number   INT         NOT NULL,
    is_played     BOOLEAN     NOT NULL DEFAULT false,
    home_score    INT         NULL,
    away_score    INT         NULL,
    played_at     TIMESTAMPTZ NULL,
    CONSTRAINT uq_scenario_year_game UNIQUE (scenario_id, year_number, game_number)
);

CREATE INDEX idx_games_scenario_year ON games(scenario_id, year_number);
CREATE INDEX idx_games_team_home ON games(home_team_id);
CREATE INDEX idx_games_team_away ON games(away_team_id);

