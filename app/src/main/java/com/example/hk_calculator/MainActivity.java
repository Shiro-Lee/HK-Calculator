package com.example.hk_calculator;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Stack;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button[] buttons = new Button[17];
    private int[] ids = new int[]{R.id.but_0, R.id.but_1, R.id.but_2, R.id.but_3, R.id.but_4, R.id.but_5,
            R.id.but_6, R.id.but_7, R.id.but_8, R.id.but_9, R.id.add, R.id.sub, R.id.mul, R.id.div, R.id.equ,
            R.id.point, R.id.clr};
    private ImageButton backBtn;
    private TextView textView_0;  //显示完整表达式的TextView
    private TextView textView_1;  //显示临时表达式及结果的TextView
    private String expression = "0"; //临时表达式
    private String complete = "";   //完整表达式
    private int state = 0;  //表达式状态，最后一位为：数字-0，运算符-1，小数点-2
    private boolean end = false;    //单次计算结束标志
    private Stack<Double> doubleStack = new Stack<>();
    private boolean divideByZero = false;   //除零错误标志
    SharedPreferences sp;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        for (int i = 0; i < ids.length; i++) {
            buttons[i] = findViewById(ids[i]);
            buttons[i].setOnClickListener(this);
        }
        backBtn = findViewById( R.id.back);
        backBtn.setOnClickListener(this);
        textView_0 = findViewById(R.id.screen_0);
        textView_1 = findViewById(R.id.screen_1);

        sp = getSharedPreferences("data",MODE_PRIVATE);
        String lastResult = sp.getString("lastResult","0");
        String lastComplete = sp.getString("lastComplete","");
        textView_0.setText(lastComplete);
        expression = lastResult;
        textView_1.setText(lastResult);
        int length = expression.length();
        char last = expression.charAt(length - 1);
        if(last == '+' || last == '-' || last == '×' || last == '÷')
            state = 1;
        else if(last=='.')
            state = 2;
        else
            end = true;
    }

    public void onClick(View view) {

        int id = view.getId(), length = expression.length();
        String btn_text = "";
        if(id!=R.id.back) {
            Button btn = findViewById(id);
            btn_text = btn.getText().toString();
        }
        char last = expression.charAt(length - 1);   //获取最后一位字符
        if (expression.length() >= 20) //表达式最长为20个字符
            if (!(id==R.id.clr) && !(id==R.id.back) && !(id==R.id.equ)) {
                Toast.makeText(this, "表达式已达最大长度（20字符）！", Toast.LENGTH_SHORT).show();
                return;
            }
        if (end) {
            textView_0.setText(""); //新一轮计算开始，清空上次计算的完整表达式
            editor = sp.edit();
            editor.putString("lastComplete","");
            editor.apply();
        }

        switch (id) {  //判断按键

            case R.id.clr:   //清屏，表达式置为0
                expression = "0";
                state = 0;
                end = false;
                break;

            case R.id.back:  //退格
                if(end)
                    end = false;
                if (length > 1) {
                    expression = expression.substring(0, length - 1); //退一格
                    length--;
                    last = expression.charAt(length - 1);
                    if (last == '+' || last == '-' || last == '×' || last == '÷')
                        state = 1;
                    else if (last == '.')
                        state = 2;
                    else state = 0;
                } else if (length == 1) {
                    expression = "0";
                    state = 0;
                }
                break;

            case R.id.point:   //小数点
                if (state == 1 || state == 2)
                    break;
                char temp;
                for (int i = length - 1; i >= 0; i--) {
                    temp = expression.charAt(i);
                    if (temp == '.')
                        break;
                    else if (i == 0 || temp == '+' || temp == '-' || temp == '×' || temp == '÷') {
                        expression += ".";
                        end = false;
                        state = 2;
                        break;
                    }
                }
                break;

            case R.id.add:
                if(end)
                    end = false;
                if (expression.equals("-"))
                    break;
                else if (state == 0)    //数字后直接加+
                    expression += '+';
                else if (state == 1 || state == 2)    //替换运算符、小数点
                    expression = expression.substring(0, length - 1) + '+';
                state = 1;
                break;

            case R.id.sub:
                if(end)
                    end = false;
                if (expression.equals("0"))
                    expression = "-";
                else if (last == '+' || state == 2)   //替换运算符、小数点
                    expression = expression.substring(0, length - 1) + '-';
                else if (state == 0 || last == '×' || last == '÷')
                    expression += "-";
                state = 1;
                break;

            case R.id.mul:
            case R.id.div:
                if(end)
                    end = false;
                if (state == 0)
                    expression += btn_text;
                else if (state == 1 || state == 2)   //替换运算符、小数点
                    expression = expression.substring(0, length - 1) + btn_text;
                state = 1;
                break;

            case R.id.equ:
                if (state == 1 || state == 2)    //表达是最后不能是运算符、小数点
                    break;
                else {  //正确计算
                    state = 0;
                    complete = addBrackets(expression); //为负数加上括号的完整表达式
                    double result = calculate(complete);
                    if(divideByZero) {  //除零错误
                        Toast.makeText(this, "除数不能为0！", Toast.LENGTH_SHORT).show();
                        divideByZero = false;
                        break;
                    }
                    textView_0.setText(complete);
                    editor = sp.edit();
                    editor.putString("lastComplete",complete);
                    editor.apply();
                    expression = format(result);
                    end = true;
                }
                break;

            default:   //数字
                if(end) {
                    expression = "";
                    end = false;
                }
                if (expression.equals("0"))
                    expression = btn_text;
                else if (expression.length() > 1 && last == '0') {
                    char temp2 = expression.charAt(expression.length() - 2);
                    //倒数第二个字符为运算符、最后一个字符为0时输入数字将把0替换
                    if (temp2 == '+' || temp2 == '-' || temp2 == '×' || temp2 == '÷')
                        expression = expression.substring(0, expression.length() - 1) + btn_text;
                    else expression += btn_text;
                } else expression += btn_text;
                state = 0;

        }
        editor = sp.edit();
        editor.putString("lastResult",expression);
        editor.apply();
        textView_1.setText(expression);

        if (expression.equals("416"))
            Toast.makeText(this, "指挥官，有我在就足够了。", Toast.LENGTH_LONG).show();
    }


    //为表达式中的负数加上括号以便后续处理
    private String addBrackets(String s) {
        boolean flag = false;   //是否正在添加括号
        if (s.charAt(0) == '-')
            s = '(' + s;
        s = s.replaceAll("×-", "×(-");
        s = s.replaceAll("÷-", "÷(-");
        for (int i = 0; i < s.length(); i++) {   //为负数加上括号
            if (s.charAt(i) == '(')
                flag = true;    //准备加右括号
            else if (flag && i < s.length() - 1) {  //在下一个运算符前加右括号
                if (s.charAt(i + 1) == '+' || s.charAt(i + 1) == '-' || s.charAt(i + 1) == '×' || s.charAt(i + 1) == '÷') {
                    s = s.substring(0, i + 1) + ')' + s.substring(i + 1);
                    flag = false;
                }
            } else if (flag) { //表达式末尾加右括号
                s += ')';
                flag = false;
            }
        }
        return s;
    }


    //计算表达式
    private double calculate(String s) {
        double result;
        ArrayList<String> convertList = getStringList(s);
        char ch;
        for(int i=0; i<convertList.size(); i++){
            ch = convertList.get(i).charAt(0);
            if(Character.isDigit(ch) || convertList.get(i).length()>1){
                doubleStack.push(Double.parseDouble(convertList.get(i)));
            }
            else
                switch (ch){
                    case '+': case'-': case'×': case'÷':
                        doOperate(ch);
                        if(divideByZero)
                            return -1;
                        break;
                }
        }
        if(!divideByZero)
            result = doubleStack.pop();
        else return -1;
        return result;
    }


    //结果最多保留小数点后9位
    private String format(double result){
        DecimalFormat df = new DecimalFormat("#.#########");
        return df.format(result);
    }


    //将表达式拆解为数字和运算符组成的列表，并返回后缀表达式
    private ArrayList<String> getStringList(String s){
        char current;
        ArrayList<String> strList = new ArrayList<>();
        String item = "";   //将要被加入ArrayList的数字/运算符
        for(int i=0; i<s.length(); i++){
            current = s.charAt(i);
            if(current=='(') {
                item += '-';
                i++;
            }
            else if(current=='.' || Character.isDigit(current))
                item += current;
            else if(current=='+'||current=='-'||current=='×'||current=='÷'){
                strList.add(item);
                strList.add(String.valueOf(current));
                item = "";
            }
        }
        strList.add(item);
        return getConvertList(strList);
    }


    //中缀表达式转换为后缀表达式
    private ArrayList<String> getConvertList(ArrayList<String> list){
        Stack<Character> stack = new Stack<>();
        char chs, chl;  //chs表示栈顶运算符，chl表示原列表运算符
        ArrayList<String> convertList = new ArrayList<>();  //保存后缀表达式
        for (int i=0; i<list.size(); i++){
            chl = list.get(i).charAt(0);
            if(Character.isDigit(chl) || list.get(i).length()>1)    //若是数字直接复制到后缀表达式列表
                convertList.add(list.get(i));
            else{
                if(stack.empty())
                    chs = '#';
                else chs = stack.peek();
                if(inside(chs)<outside(chl))
                    stack.push(chl);
                else {
                    convertList.add(String.valueOf(stack.pop()));
                    i--;
                }
            }
        }
        while(!stack.empty())
            convertList.add(String.valueOf(stack.pop()));
        return convertList;
    }


    //对运算符op取操作数进行运算
    private void doOperate(char op){
        double left, right, value=0;
        right = doubleStack.pop();
        left = doubleStack.pop();
        switch (op){
            case '+':
                value = left + right;
                break;
            case '-':
                value = left - right;
                break;
            case '×':
                value = left * right;
                break;
            case '÷':
                if(right==0) {
                    divideByZero = true;
                    doubleStack.clear();
                    return;
                }
                value = left/right;
                break;
        }
        doubleStack.push(value);
    }


    //栈内优先级
    private int inside(char c) {
        switch (c) {
            case '#':
                return 0;
            case '+': case '-':
                return 2;
            case '×': case '÷':
                return 4;
            default: return -1;
        }
    }

    //栈外优先级
    private int outside(char c) {
        switch (c) {
            case '#':
                return 0;
            case '+': case '-':
                return 1;
            case '×': case '÷':
                return 3;
            default: return -1;
        }
    }

}