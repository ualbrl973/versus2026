export type GameMode =
  | 'SURVIVAL'
  | 'PRECISION'
  | 'BINARY_DUEL'
  | 'PRECISION_DUEL'
  | 'SABOTAGE';

export type MatchStatus = 'WAITING' | 'IN_PROGRESS' | 'FINISHED';

export interface PlayerInLobby {
  userId: string;
  username: string;
  avatarUrl: string | null;
  ready: boolean;
}

export interface MatchCreated {
  matchId: string;
  mode: GameMode;
  roomCode: string;
}

export interface LobbyState {
  matchId: string;
  mode: GameMode;
  status: MatchStatus;
  players: PlayerInLobby[];
  requiredPlayers: number;
  roomCode: string;
}

export interface MatchFoundPayload {
  matchId: string;
  mode: GameMode;
  opponents: PlayerInLobby[];
}

export interface PlayerJoinedPayload {
  player: PlayerInLobby;
}

export interface PlayerLeftPayload {
  userId: string;
}

export interface PlayerReadyPayload {
  userId: string;
  ready: boolean;
}

export interface MatchStartingPayload {
  countdownSeconds: number;
}

export interface MatchStartPayload {
  matchId: string;
  mode: GameMode;
}
