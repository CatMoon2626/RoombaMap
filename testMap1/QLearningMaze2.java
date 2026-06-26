import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class QLearningMaze2 {

    public static void main(String[] args) {
        // 障害物を3つ配置した盤面を作成
        // ※注意: ランダム配置のため、ゴールへの道が完全に塞がれる「クリア不可能な盤面」が生成されることもあります。
        // その場合は何度か再実行してください。
        Environment env = new Environment(3);
        
        System.out.println("【初期盤面】（Sがスタート、Xが障害物、Fが通過済み）");
        env.printBoard();

        Agent agent = new Agent(env);
        
        System.out.println("\n学習を開始します...");
        agent.train(2000); // 2000エピソード学習
        System.out.println("学習が完了しました！\n");

        System.out.println("【学習済みのエージェントによるテスト走行】");
        agent.test();
    }
}

// --- 環境（盤面）クラス ---
class Environment {
    final int LENGTH = 5;
    final int WIDTH = 5;
    final int SIZE = LENGTH * WIDTH;
    
    int[][] map = new int[LENGTH][WIDTH];
    int agentRow = 0;
    int agentCol = 0;

    int count = 0;
    int countMax = SIZE; 

    public Environment(int block) {
        ArrayList<Integer> list = new ArrayList<>();
        for (int i = 1; i <= SIZE - 2; i++) {
            list.add(i);
        }
        for (int i = 0; i < block; i++) {
            Collections.shuffle(list);
            int id = list.remove(0);
            int col = id / LENGTH;
            int row = id % WIDTH;
            map[col][row] = 2; // 障害物
        }
        map[0][0] = 3; //スタート
        countMax = SIZE - block - 1; //ブロックとスタート以外のマスの数
    }

    // エージェントの位置を初期化
    public int reset() {
        agentRow = 0;
        agentCol = 0;
        reset_env();
        return getState();
    }

    // 現在の状態（0〜24のインデックス）を取得
    public int getState() {
        return agentRow * WIDTH + agentCol;
    }

    // 行動（0:上, 1:下, 2:左, 3:右）を受け取り、結果を返す
    public StepResult step(int action) {
        int nextRow = agentRow;
        int nextCol = agentCol;

        if (action == 0) nextRow--; // 上
        else if (action == 1) nextRow++; // 下
        else if (action == 2) nextCol--; // 左
        else if (action == 3) nextCol++; // 右

        // 盤面外に出ようとした場合（壁への衝突）
        if (nextRow < 0 || nextRow >= LENGTH || nextCol < 0 || nextCol >= WIDTH) {
            return new StepResult(getState(), -10, false); // 動かずペナルティ
        }

        // 移動を反映
        agentRow = nextRow;
        agentCol = nextCol;
        int nextState = getState();

        // 移動先のマスに応じた報酬と終了判定
        if (map[agentRow][agentCol] == 3 && count == countMax) {
            return new StepResult(nextState, 100, true);  // ゴール！
        } else if (map[agentRow][agentCol] == 2) {
            return new StepResult(nextState, -100, true); // 障害物に衝突して終了（死亡）
        } else if(map[agentRow][agentCol] == 0){ //まだ通っていないところ
            map[agentRow][agentCol] = 1; //通った判定にする
            count++; //カウント
            return new StepResult(nextState, 0, false);
        } else {
            return new StepResult(nextState, -1, false);  // 通常の移動（時間ペナルティ）
        }
    }

    // 現在の盤面を描画する（エージェントの現在位置に○を表示）
    public void printBoard() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LENGTH; i++) {
            for (int j = 0; j < WIDTH; j++) {
                if (i == agentRow && j == agentCol) {
                    sb.append("O"); // エージェントの現在位置
                } else {
                    switch (map[i][j]) {
                        case 0: sb.append(" "); break; // 空白
                        case 1: sb.append("F"); break; // 障害物
                        case 2: sb.append("X"); break; // 障害物
                        case 3: sb.append("S"); break; // ゴール
                    }
                }
            }
            sb.append("\n");
        }
        System.out.print(sb.toString());
        System.out.println("---------");
    }

    //盤面をリセット
    public void reset_env(){
        for (int i = 0; i < SIZE; i++) {
            int col = i / LENGTH;
            int row = i % WIDTH;
            if(map[col][row]==1)
                map[col][row] = 0; //探索済みをもとに戻す
        }
    }
}

// --- ステップ実行結果を保持するクラス ---
class StepResult {
    int nextState;
    double reward;
    boolean done;

    public StepResult(int nextState, double reward, boolean done) {
        this.nextState = nextState;
        this.reward = reward;
        this.done = done;
    }
}

// --- Q学習を行うエージェントクラス ---
class Agent {
    Environment env;
    double[][] qTable;
    Random rand = new Random();

    // ハイパーパラメータ
    double alpha = 0.1;   // 学習率（新しい情報をどれくらい信じるか）
    double gamma = 0.9;   // 割引率（将来の報酬をどれくらい重視するか）
    double epsilon = 0.1; // 探索率（ランダムに行動して未知の道を探す確率）
    final int loopMax = 100; //最大の探索回数

    public Agent(Environment env) {
        this.env = env;
        this.qTable = new double[env.SIZE][4]; // 25状態 × 4行動
    }

    // 学習メインループ
    public void train(int episodes) {
        for (int e = 0; e < episodes; e++) {
            int state = env.reset();
            boolean done = false;

            while (!done) {
                int action = chooseAction(state, epsilon);
                StepResult result = env.step(action);

                // Q値の更新式（ベルマン方程式に基づく）
                double maxNextQ = getMaxValue(qTable[result.nextState]);
                qTable[state][action] = qTable[state][action] + alpha * (result.reward + gamma * maxNextQ - qTable[state][action]);

                state = result.nextState;
                done = result.done;
            }
        }
    }

    // 学習結果を使って実際にゴールを目指す
    public void test() {
        int state = env.reset();
        boolean done = false;
        int stepCount = 0;

        env.printBoard();

        while (!done && stepCount < loopMax) { // 無限ループ防止のため最大値を決める
            // テスト時は探索（ランダム行動）を行わず、常にQ値が最大の行動を選ぶ
            int action = chooseAction(state, 0.0); 
            StepResult result = env.step(action);
            
            System.out.println((stepCount + 1) + "歩目:");
            env.printBoard();

            if (result.reward == 100) {
                System.out.println("ゴールに到達しました！");
            } else if (result.reward == -100) {
                System.out.println("障害物にぶつかりました...（学習不足か、クリア不可能な盤面です）");
            }

            state = result.nextState;
            done = result.done;
            stepCount++;
        }
        
        if (!done) {
            System.out.println("ゴールにたどり着けませんでした。");
        }
    }

    // ε-greedy法による行動選択
    private int chooseAction(int state, double epsilon) {
        if (rand.nextDouble() < epsilon) {
            return rand.nextInt(4); // ランダムな行動（探索）
        } else {
            // Q値が最大の行動を選択（利用）
            int bestAction = 0;
            double maxQ = qTable[state][0];
            for (int i = 1; i < 4; i++) {
                if (qTable[state][i] > maxQ) {
                    maxQ = qTable[state][i];
                    bestAction = i;
                }
            }
            return bestAction;
        }
    }

    // 配列内の最大値を取得
    private double getMaxValue(double[] array) {
        double max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) max = array[i];
        }
        return max;
    }
}