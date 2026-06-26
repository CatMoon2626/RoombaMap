import java.util.ArrayList;
import java.util.Collections;

class Board{
    final int LENGTH = 5; //縦の長さ
    final int WIDTH = 5; //横の長さ
    final int SIZE = LENGTH * WIDTH;
    /**
     0がただのマス、1がプレイヤー、2が障害物、3がゴール
     */
    int[][] map = new int[LENGTH][WIDTH];

    public Board(int block){
        ArrayList<Integer> list = new ArrayList<Integer>();
        // listに値を入れる。この段階では昇順
        for(int i = 1 ; i <= SIZE-2 ; i++) {
            list.add(i);
        }
        //blockの数だけ障害物を生成
        for(int i=0;i<block;i++){
            Collections.shuffle(list);
            int id = list.remove();
            int col = id/LENGTH;
            int row = id%WIDTH;
            map[col][row] = 2;
        }
        map[0][0]=1;
        map[LENGTH-1][WIDTH-1]=3;

    }
    public int get(int col, int row){return map[col][row];}
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LENGTH; i++) {
            for (int j = 0; j < WIDTH; j++) {
                switch (map[i][j]) {
                    case 0:
                        sb.append(" "); // 空白（全角スペース）
                        break;
                    case 1:
                        sb.append("○");  // プレイヤー
                        break;
                    case 2:
                        sb.append("☓");  // 障害物
                        break;
                    case 3:
                        sb.append("□");  // ゴール
                        break;
                }
            }
            sb.append("\n"); // 1行終わるごとに改行
        }
        return sb.toString();
    }
}