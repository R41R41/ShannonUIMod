package com.shannon.ui.gfx;

import com.shannon.ui.state.BotStatus;

/** The mod's pixel icons. Shapes are drawn to sit beside vanilla's 8-pixel font. */
public final class Icons {
    private static final int HANDLE = 0xFF9A6A35;

    // ===== Status =====

    public static final PixelIcon PICKAXE = PixelIcon.of(new Object[]{'#', Palette.YELLOW, 'h', HANDLE},
            ".#######.",
            "##..h..##",
            "#...h...#",
            "....h....",
            "....h....",
            "....h....",
            "....h....",
            "....h....",
            "....h....");

    public static final PixelIcon SLEEP = PixelIcon.of(new Object[]{'#', Palette.GRAY},
            ".........",
            ".........",
            "..#####..",
            ".....#...",
            "....#....",
            "...#.....",
            "..#####..",
            ".........",
            ".........");

    public static final PixelIcon QUESTION = PixelIcon.of(new Object[]{'#', Palette.AQUA, 'q', 0xFF0A3C3C},
            ".#######.",
            "###qqq###",
            "##q###q##",
            "#####q###",
            "####q####",
            "#########",
            "####q####",
            ".#######.",
            "..##.....",
            "..#......");

    public static final PixelIcon WARNING = PixelIcon.of(new Object[]{'#', Palette.RED, 'q', 0xFF3F0000},
            "....#....",
            "...###...",
            "...#q#...",
            "..##q##..",
            "..##q##..",
            ".#######.",
            ".###q###.",
            "#########");

    // ===== Task steps =====

    public static final PixelIcon CHECK = PixelIcon.of(new Object[]{'#', Palette.GREEN},
            "........#",
            ".......##",
            "#.....##.",
            "##...##..",
            ".##.##...",
            "..###....",
            "...#.....");

    public static final PixelIcon PLAY = PixelIcon.of(new Object[]{'#', Palette.YELLOW},
            "..#....",
            "..##...",
            "..###..",
            "..####.",
            "..#####",
            "..####.",
            "..###..",
            "..##...",
            "..#....");

    public static final PixelIcon PENDING = PixelIcon.of(new Object[]{'#', Palette.GRAY},
            "..####..",
            ".#....#.",
            "#......#",
            "#......#",
            "#......#",
            "#......#",
            ".#....#.",
            "..####..");

    // ===== Direction to the bot: index 0 points ahead, then clockwise in eighths =====

    private static final PixelIcon ARROW_UP = PixelIcon.of(new Object[]{'#', Palette.WHITE},
            "...#...",
            "..###..",
            ".#####.",
            "#######",
            "..###..",
            "..###..",
            "..###..");

    private static final PixelIcon ARROW_UP_RIGHT = PixelIcon.of(new Object[]{'#', Palette.WHITE},
            "...####",
            "....###",
            "...####",
            "..###.#",
            ".###...",
            "###....",
            ".#.....");

    public static final PixelIcon[] ARROWS = {
            ARROW_UP,
            ARROW_UP_RIGHT,
            ARROW_UP.rotatedClockwise(),
            ARROW_UP_RIGHT.rotatedClockwise(),
            ARROW_UP.rotatedClockwise().rotatedClockwise(),
            ARROW_UP_RIGHT.rotatedClockwise().rotatedClockwise(),
            ARROW_UP.rotatedClockwise().rotatedClockwise().rotatedClockwise(),
            ARROW_UP_RIGHT.rotatedClockwise().rotatedClockwise().rotatedClockwise(),
    };

    // ===== Command switcher =====

    public static final PixelIcon CMD_STOP = PixelIcon.of(new Object[]{'#', 0xFFD83A3A, 'w', Palette.WHITE},
            "...######...",
            "..########..",
            ".##########.",
            "############",
            "############",
            "##wwwwwwww##",
            "##wwwwwwww##",
            "############",
            "############",
            ".##########.",
            "..########..",
            "...######...");

    public static final PixelIcon CMD_FOLLOW = PixelIcon.of(new Object[]{'#', Palette.XP},
            "............",
            "##....##....",
            "##....##....",
            "..##....##..",
            "..##....##..",
            "....##....##",
            "....##....##",
            "..##....##..",
            "..##....##..",
            "##....##....",
            "##....##....",
            "............");

    public static final PixelIcon CMD_COME = PixelIcon.of(new Object[]{'#', Palette.AQUA},
            "####....####",
            "#..........#",
            "#..........#",
            "#..........#",
            "....####....",
            "....####....",
            "....####....",
            "....####....",
            "#..........#",
            "#..........#",
            "#..........#",
            "####....####");

    public static final PixelIcon CMD_RESUME = PixelIcon.of(new Object[]{'#', Palette.YELLOW},
            "..##........",
            "..####......",
            "..####......",
            "..######....",
            "..######....",
            "..########..",
            "..########..",
            "..######....",
            "..######....",
            "..####......",
            "..####......",
            "..##........");

    public static final PixelIcon CMD_CANCEL = PixelIcon.of(new Object[]{'#', 0xFFC8C8C8},
            "##........##",
            "##........##",
            "..##....##..",
            "..##....##..",
            "....####....",
            "....####....",
            "....####....",
            "....####....",
            "..##....##..",
            "..##....##..",
            "##........##",
            "##........##");

    public static final PixelIcon CMD_BAG = PixelIcon.of(
            new Object[]{'b', 0xFF9C6B30, 'd', 0xFF5A3A16, 'l', 0xFFD8D8D8},
            "............",
            "............",
            ".bbbbbbbbbb.",
            "bbbbbbbbbbbb",
            "bbbbbbbbbbbb",
            "bbbbbllbbbbb",
            "dddddllddddd",
            "bbbbbllbbbbb",
            "bbbbbbbbbbbb",
            "bbbbbbbbbbbb",
            "bbbbbbbbbbbb",
            "dddddddddddd");

    // ===== Pointing at something =====

    public static final PixelIcon CMD_DIG = PixelIcon.of(new Object[]{'#', 0xFFE6E6E6, 's', 0xFF9C9C9C, 'h', 0xFF7A5230},
            "............",
            "....#####...",
            "...#sssss#..",
            "..#s....hs#.",
            "........h.s#",
            ".......h...#",
            "......h.....",
            ".....h......",
            "....h.......",
            "...h........",
            "..h.........",
            ".h..........");

    public static final PixelIcon CMD_GATHER = PixelIcon.of(new Object[]{'#', 0xFF9C6B30, 'd', 0xFF5A3A16},
            "............",
            "...######...",
            "...#d##d#...",
            "...#d##d#...",
            "...######...",
            "............",
            "######.#####",
            "#d##d#.#d##d",
            "#d##d#.#d##d",
            "######.#####",
            "............",
            "............");

    public static final PixelIcon CMD_WAIT = PixelIcon.of(new Object[]{'#', Palette.YELLOW, 's', 0xFFE2C46A},
            "############",
            ".#........#.",
            ".#ssssssss#.",
            "..#ssssss#..",
            "...#ssss#...",
            "....#ss#....",
            "....#..#....",
            "...#....#...",
            "..#..ss..#..",
            ".#.ssssss.#.",
            ".#ssssssss#.",
            "############");

    public static final PixelIcon CMD_ATTACK = PixelIcon.of(new Object[]{'#', 0xFFE6E6E6, 'h', 0xFF7A5230, 'g', 0xFFC8A000},
            "..........##",
            ".........###",
            "........###.",
            ".......###..",
            "......###...",
            ".....###....",
            "..g.###.....",
            "...g##......",
            "...hg.......",
            "..h..g......",
            ".h..........",
            "h...........");

    // ===== Voice =====

    public static final PixelIcon MIC_ON = PixelIcon.of(new Object[]{'#', Palette.RED, 'w', Palette.WHITE},
            "..###..",
            ".#####.",
            ".#####.",
            ".#####.",
            ".#####.",
            "w.###.w",
            "w.....w",
            ".wwwww.",
            "...w...",
            ".wwwww.");

    public static final PixelIcon MIC_OFF = PixelIcon.of(new Object[]{'#', Palette.GRAY, 'w', Palette.DARK_GRAY},
            "..###..",
            ".#####.",
            ".#####.",
            ".#####.",
            ".#####.",
            "w.###.w",
            "w.....w",
            ".wwwww.",
            "...w...",
            ".wwwww.");

    private Icons() {
    }

    public static PixelIcon status(BotStatus status) {
        return switch (status) {
            case IDLE -> SLEEP;
            case WORKING -> PICKAXE;
            case WAITING -> QUESTION;
            case ERROR -> WARNING;
        };
    }

    public static int statusColor(BotStatus status) {
        return switch (status) {
            case IDLE -> Palette.GRAY;
            case WORKING -> Palette.YELLOW;
            case WAITING -> Palette.AQUA;
            case ERROR -> Palette.RED;
        };
    }

    /** The arrow for a target {@code relativeYaw} degrees to the right of where the player looks. */
    public static PixelIcon arrow(float relativeYaw) {
        int index = Math.floorMod(Math.round(relativeYaw / 45f), 8);
        return ARROWS[index];
    }
}
