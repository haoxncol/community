package com.nowcoder.community.util;


import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {
    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

    //替换符
    private static final  String REPLACEMENT = "***";

    //根节点
    private TrieNode rootNode = new TrieNode();

    @PostConstruct
    public void init(){
        try(
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        ){
            String keyword;
            while((keyword = reader.readLine()) != null){
                //添加到前缀树
                this.addKeyword(keyword);
            }
        }catch (IOException e){
            logger.error("加载敏感词失败" + e.getMessage());
        }

    }

    //将一个敏感词添加到前缀树中
    private void addKeyword(String keyword){
        TrieNode tempNode = rootNode;
        for(int i = 0 ; i < keyword.length(); i++){
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);
            if(subNode ==null){
                //初始化子节点
                subNode = new TrieNode();
                tempNode.addSubNode(c , subNode );
            }
            // 指向子节点进入下一轮循环
            tempNode = subNode;

            //设置结束的标识
            if(i == keyword.length() - 1){
                tempNode.setKeywordEnd(true);
            }
        }
    }


    /**
     * 过滤敏感词
     * @param text 等待过滤的文本
     * @return 过滤猴的文本
     */
    public String filter(String text){
        if(StringUtils.isBlank(text)){
            return null;
        }
        //指针1：指向树
        TrieNode tempNode = rootNode;
        //指针2
        int begin = 0;
        //指针3
        int position = 0;
        //结果
        StringBuilder sb = new StringBuilder();
        //指针3到字符串结尾时结束遍历
        while(position < text.length()){
            char c = text.charAt(position);
            //跳过符号
            if(isSymbol(c)){
                //如果指针1处于根节点，将次符号计入结果，让指针2向下走。
                //因为指针1处于根节点，代表目标字符串的第一次遍历或者目标字符串已经成功遍历了一部分
                if(tempNode == rootNode){
                    //添加到结果字符串中
                    sb.append(c);
                    //将begin后移，以一个新的单词过滤
                    begin++;
                }
                //如果当前节点不是根节点，说明符号字符后的字符还需要继续过滤
                position++;
                continue;
            }
            //判断当前节点的子节点是否有目标字符c
            tempNode = tempNode.getSubNode(c);
            if(tempNode == null){
                sb.append(text.charAt(begin));
                begin = ++position;
                tempNode = rootNode;
            }
            else if(tempNode.isKeywordEnd()){
                //如果找到了子节点且子节点是敏感词的结尾
                //则当前begin-position之间的字符串为敏感词，替换
                sb.append(REPLACEMENT);
                //将begin指针的位置移动到position的下一位
                begin = ++position;
                tempNode = rootNode;
            }
            else if(position + 1 == text.length()){
                //特殊情况
                //虽然position指向的字符在树中存在，但不是敏感词的结尾，且此时position到了字符串的末位
                //因此begin-position之间的字符串不是敏感词  但begin+1 到position之间可能是为敏感词
                //只将begin加入到结果中
                sb.append(text.charAt(begin));
                position = ++begin;
                tempNode = rootNode;
            }
            else{ //position指向的字符在树中存在，但不是敏感词结尾，并且postion没到字符串末
                position++;
            }

        }
        sb.append(text.substring(begin));
        return sb.toString();
    }

    //判断是否为符号
    private boolean isSymbol (Character c){
        // 从0x2E80 ~ 0x9FFF 是东亚文字范围
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }

    //前缀树
    private class TrieNode{

        //关键词结束的标识
        private boolean isKeywordEnd = false;

        //子节点(key是下级字符，value是下级节点)
        private Map<Character , TrieNode> subnodes = new HashMap<>();

        //添加子节点
        public void addSubNode(Character c, TrieNode node){
            subnodes.put(c,node);
        }


        public TrieNode getSubNode(Character c){
            return subnodes.get(c);
        }


        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }
    }

}
