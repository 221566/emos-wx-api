package shixun1;

import java.util.Scanner;

public class Shixun1 {

    public static void main(String[] args) {
        // 输入一个数，判断是奇数还是偶数
        Scanner input = new Scanner(System.in);
        System.out.println("请输入一个整数：");
        try {
            int zhengshu = input.nextInt();
            if (zhengshu % 2 == 0) {
                System.out.println("您输入的" + zhengshu + "是一个偶数！");
            } else {
                System.out.println("您输入的" + zhengshu + "是一个奇数！");
            }

        } catch (Exception e) {
            System.out.println("您的输入有误！");
            Shixun1();
        }


    }

    private static void Shixun1() {
        // TODO Auto-generated method stub

    }

}