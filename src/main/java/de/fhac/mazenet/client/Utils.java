package de.fhac.mazenet.client;

import java.util.ArrayList;
import java.util.List;

import de.fhac.mazenet.server.game.Board;
import de.fhac.mazenet.server.game.Card;
import de.fhac.mazenet.server.game.Position;
import de.fhac.mazenet.server.generated.CardData;
import de.fhac.mazenet.server.generated.CardData.Openings;
import de.fhac.mazenet.server.generated.MoveMessageData;
import de.fhac.mazenet.server.generated.PositionData;
import de.fhac.mazenet.server.generated.Treasure;

public interface Utils {

	public static final int LENGTH = 7;

	// Diese Methode ermöglicht es, einen Punkt in der Nähe des Ziels zu finden, um sich diesem anzunähern
	public static MoveMessageData getClosePointToTarget(Board board, Treasure treasure, List<MoveMessageData> moves,
			int playerId) {
		for (MoveMessageData move : moves) {
			Board nextBoard = board.fakeShift(move);

			PositionData treasurePositionData = nextBoard.findTreasure(treasure);

			if (treasurePositionData != null) {
				Position treasurePosition = new Position(treasurePositionData);
				Position playerPosition = new Position(board.findPlayer(playerId));

				// In die Nähe des Ziels zielen

				int targetRow = treasurePosition.getRow();
				int targetCol = treasurePosition.getCol();
				int startX = targetRow == 0 ? 0 : targetRow - 1;
				int startY = targetCol == 0 ? 0 : targetCol - 1;
				int endX = targetRow == LENGTH - 1 ? targetRow : targetRow + 1;
				int endY = targetCol == LENGTH - 1 ? targetCol : targetCol + 1;

				List<Path> paths = new ArrayList<Path>();
				for (int row = startX; row <= endX; row++) {
					for (int col = startY; col <= endY; col++) {
						if (row != targetRow && col != targetCol) {
							Position position = new Position(row, col);
							if (position.getRow() != playerPosition.getRow()
									|| position.getCol() != playerPosition.getCol()) {
								if (nextBoard.pathPossible(playerPosition, position)) {
									move.setNewPinPos(position);
									Path path = new Path(playerPosition, treasurePosition);
									path.emfiler(move);
									paths.add(path);
								}
							} else {
								return null;
							}
						}
					}
				}
				if (paths.size() != 0) {
					Path path = Path.getShortestPath(paths);
					MoveMessageData nextMove = path.defiler();
					return nextMove;
				} else {
					return null;
				}

			}
		}
		return null;
	}

	// Diese Methode ermöglicht es, alle möglichen Züge eines Spielers auf einem bestimmten Spielbrett zu erhalten
	public static List<MoveMessageData> getAllMoveMessageData(Board board, int playerId) {
		List<Position> potentialShiftMoves = Position.getPossiblePositionsForShiftcard();
		potentialShiftMoves.remove(board.getForbidden());

		List<MoveMessageData> potentialMoves = new ArrayList<MoveMessageData>();
		for (Position position : potentialShiftMoves) {
			List<CardData> orientedShiftCards = getAllOrientationPossibilities(board.getShiftCard());
			for (CardData orientedShiftCard : orientedShiftCards) {
				Position playerPosition;
				MoveMessageData potentialMove = new MoveMessageData();
				potentialMove.setShiftPosition(position);
				potentialMove.setShiftCard(orientedShiftCard);
				Board boardNext = board.fakeShift(potentialMove);
				playerPosition = boardNext.findPlayer(playerId);
				potentialMove.setNewPinPos(playerPosition);
				potentialMoves.add(potentialMove);
			}
		}
		return potentialMoves;
	}

	// Mit dieser Methode können alle möglichen Zustände des Spielbretts ermittelt werden, indem die Bewegungen der Liste myMoves ausgeführt werden
	public static List<Board> getAllBoardStatus(Board board, List<MoveMessageData> myMoves) {
		List<Board> boards = new ArrayList<Board>();
		for (MoveMessageData myMove : myMoves) {
			boards.add(board.fakeShift(myMove));
		}
		return boards;
	}

	// Mit dieser Methode können alle möglichen Ausrichtungen für eine bestimmte Karte ermittelt werden
	public static List<CardData> getAllOrientationPossibilities(CardData shiftCard) {
		List<CardData> shiftCards = new ArrayList<CardData>();
		shiftCards.add(shiftCard);

		Card card = new Card(shiftCard);
		Openings openings = card.getOpenings();
		if (card.getShape() == Card.CardShape.I) {
			openings.setBottom(!openings.isBottom());
			openings.setLeft(!openings.isLeft());
			openings.setRight(!openings.isRight());
			openings.setTop(!openings.isTop());
			Card card2 = new Card(card);
			shiftCards.add((CardData) card2);
		} else {
			if (card.getOrientation() != Card.Orientation.D0) {
				shiftCards.add((CardData) new Card(card.getShape(), Card.Orientation.D0, card.getTreasure()));
			}
			if (card.getOrientation() != Card.Orientation.D90) {
				shiftCards.add((CardData) new Card(card.getShape(), Card.Orientation.D90, card.getTreasure()));
			}
			if (card.getOrientation() != Card.Orientation.D180) {
				shiftCards.add((CardData) new Card(card.getShape(), Card.Orientation.D180, card.getTreasure()));
			}
			if (card.getOrientation() != Card.Orientation.D270) {
				shiftCards.add((CardData) new Card(card.getShape(), Card.Orientation.D270, card.getTreasure()));
			}
		}
		return shiftCards;
	}
}
