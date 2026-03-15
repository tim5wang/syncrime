// 个人知识库系统完整演示
// 展示系统的核心功能和使用方法

import {
  KnowledgeBaseManager,
  TextKnowledgeExtractor,
  ContentType,
  Priority,
  KnowledgeItem,
  SearchResult,
  Recommendation
} from './knowledge-base-core';

import {
  KnowledgeTipEngine,
  IntelligentCompletionEngine,
  ContextualRecommendationEngine,
  LearningAssistanceEngine,
  InputContext,
  KnowledgeTip,
  CompletionSuggestion,
  ContextualRecommendation,
  LearningAssistance
} from './input-method-integration';

// ==================== 系统演示类 ====================

export class PersonalKnowledgeSystemDemo {
  private knowledgeBase: KnowledgeBaseManager;
  private tipEngine: KnowledgeTipEngine;
  private completionEngine: IntelligentCompletionEngine;
  private recommendationEngine: ContextualRecommendationEngine;
  private learningEngine: LearningAssistanceEngine;

  constructor() {
    this.knowledgeBase = new KnowledgeBaseManager();
    this.tipEngine = new KnowledgeTipEngine(this.knowledgeBase, null);
    this.completionEngine = new IntelligentCompletionEngine(this.knowledgeBase, null);
    this.recommendationEngine = new ContextualRecommendationEngine(this.knowledgeBase);
    this.learningEngine = new LearningAssistanceEngine(this.knowledgeBase);
  }

  // ==================== 完整系统演示 ====================

  async runCompleteDemo(): Promise<void> {
    console.log('🚀 个人知识库系统完整演示开始\n');

    try {
      // 1. 知识采集演示
      await this.demonstrateKnowledgeCollection();
      
      // 2. 知识管理演示
      await this.demonstrateKnowledgeManagement();
      
      // 3. 智能搜索演示
      await this.demonstrateIntelligentSearch();
      
      // 4. 输入法集成演示
      await this.demonstrateInputMethodIntegration();
      
      // 5. 学习辅助演示
      await this.demonstrateLearningAssistance();
      
      // 6. 系统统计演示
      await this.demonstrateSystemStatistics();

      console.log('\n✅ 个人知识库系统演示完成');

    } catch (error) {
      console.error('❌ 演示过程中出现错误:', error);
    }
  }

  // ==================== 知识采集演示 ====================

  private async demonstrateKnowledgeCollection(): Promise<void> {
    console.log('📚 === 知识采集演示 ===');

    // 演示不同类型的知识采集
    const knowledgeSamples = [
      {
        title: 'TypeScript基础概念',
        content: `TypeScript是JavaScript的超集，由微软开发和维护。它添加了静态类型检查、类、接口、泛型等特性。

主要特性：
1. 静态类型检查
2. 面向对象编程
3. 接口和泛型
4. 装饰器支持
5. 工具链完善

TypeScript代码会被编译成JavaScript，可以在任何支持JavaScript的环境中运行。`,
        contentType: ContentType.TEXT,
        source: '技术文档'
      },
      {
        title: '机器学习基础',
        content: `机器学习是人工智能的一个重要分支，它使计算机能够在没有明确编程的情况下学习和改进。

主要类型：
- 监督学习：使用标记数据训练模型
- 无监督学习：从未标记数据中发现模式
- 强化学习：通过奖励和惩罚学习策略

常用算法：
1. 线性回归
2. 决策树
3. 神经网络
4. 支持向量机
5. K-means聚类`,
        contentType: ContentType.TEXT,
        source: '学习笔记'
      },
      {
        title: '项目管理最佳实践',
        content: `有效的项目管理是项目成功的关键因素。

核心原则：
• 明确的目标和范围
• 合理的时间和资源规划
• 持续的风险管理
• 有效的团队沟通
• 定期的进度评估

常用方法论：
- 瀑布模型
- 敏捷开发
- Scrum框架
- 看板方法

工具推荐：
Jira, Trello, Asana, Microsoft Project`,
        contentType: ContentType.TEXT,
        source: '工作经验'
      },
      {
        title: 'React Hooks详解',
        content: `React Hooks是React 16.8引入的新特性，允许在函数组件中使用状态和其他React特性。

常用Hooks：
1. useState - 状态管理
2. useEffect - 副作用处理
3. useContext - 上下文消费
4. useReducer - 复杂状态管理
5. useMemo - 性能优化
6. useCallback - 函数缓存

示例代码：
\`\`\`javascript
import React, { useState, useEffect } from 'react';

function Counter() {
  const [count, setCount] = useState(0);
  
  useEffect(() => {
    document.title = \`Count: \${count}\`;
  }, [count]);
  
  return (
    <div>
      <p>You clicked {count} times</p>
      <button onClick={() => setCount(count + 1)}>
        Click me
      </button>
    </div>
  );
}
\`\`\``,
        contentType: ContentType.CODE,
        source: '技术文档'
      }
    ];

    console.log('\n📝 添加知识条目...');
    
    for (const sample of knowledgeSamples) {
      const item = await this.knowledgeBase.addKnowledgeItem(
        sample.title,
        sample.content,
        sample.contentType,
        sample.source
      );
      
      console.log(`✅ 已添加: ${item.title}`);
      console.log(`   📂 分类: ${item.category}`);
      console.log(`   🏷️  标签: [${item.tags.join(', ')}]`);
      console.log(`   ⭐ 质量: ${item.quality.toFixed(2)}`);
      console.log(`   📊 优先级: ${item.priority}\n`);
    }

    console.log('📚 知识采集演示完成\n');
  }

  // ==================== 知识管理演示 ====================

  private async demonstrateKnowledgeManagement(): Promise<void> {
    console.log('🔧 === 知识管理演示 ===');

    // 获取所有知识条目
    const allItems = this.knowledgeBase.getAllKnowledgeItems();
    console.log(`\n📊 当前知识库统计: ${allItems.length} 个条目`);

    // 演示分类管理
    const categories = this.knowledgeBase.getCategories();
    console.log('\n📂 知识分类:');
    categories.forEach(cat => {
      console.log(`  • ${cat.name}: ${cat.itemCount} 个条目`);
    });

    // 演示标签管理
    const tags = this.knowledgeBase.getTags();
    console.log('\n🏷️  热门标签:');
    const topTags = tags
      .sort((a, b) => b.usageCount - a.usageCount)
      .slice(0, 10);
    topTags.forEach(tag => {
      console.log(`  • ${tag.name}: ${tag.usageCount} 次使用`);
    });

    // 演示知识更新
    console.log('\n✏️  演示知识更新...');
    const firstItem = allItems[0];
    if (firstItem) {
      const updatedItem = await this.knowledgeBase.updateKnowledgeItem(
        firstItem.id,
        {
          priority: Priority.HIGH,
          tags: [...firstItem.tags, '重要', '演示']
        }
      );
      
      if (updatedItem) {
        console.log(`✅ 已更新: ${updatedItem.title}`);
        console.log(`   📊 新优先级: ${updatedItem.priority}`);
        console.log(`   🏷️  新标签: [${updatedItem.tags.join(', ')}]`);
      }
    }

    // 演示相关知识推荐
    console.log('\n🔗 相关知识推荐演示...');
    if (firstItem) {
      const relatedItems = await this.knowledgeBase.getRelatedItems(firstItem.id, 3);
      console.log(`📖 与 "${firstItem.title}" 相关的知识:`);
      relatedItems.forEach(item => {
        console.log(`  • ${item.title} (${item.category})`);
      });
    }

    console.log('\n🔧 知识管理演示完成\n');
  }

  // ==================== 智能搜索演示 ====================

  private async demonstrateIntelligentSearch(): Promise<void> {
    console.log('🔍 === 智能搜索演示 ===');

    const searchQueries = [
      'TypeScript',
      '机器学习',
      'React',
      '项目管理',
      '算法'
    ];

    for (const query of searchQueries) {
      console.log(`\n🔍 搜索: "${query}"`);
      
      const results = await this.knowledgeBase.searchKnowledge(query, { limit: 3 });
      
      if (results.length === 0) {
        console.log('  ❌ 未找到相关内容');
        continue;
      }

      console.log(`  📊 找到 ${results.length} 个结果:`);
      results.forEach((result, index) => {
        console.log(`  ${index + 1}. ${result.item.title}`);
        console.log(`     📂 分类: ${result.item.category}`);
        console.log(`     ⭐ 评分: ${result.score.toFixed(2)}`);
        console.log(`     🏷️  标签: [${result.item.tags.join(', ')}]`);
        
        if (result.highlights.length > 0) {
          console.log(`     💡 高亮: ${result.highlights[0]}`);
        }
        console.log('');
      });
    }

    // 演示个性化推荐
    console.log('\n🎯 个性化推荐演示...');
    const recommendations = await this.knowledgeBase.getRecommendations('demo_user', 5);
    
    if (recommendations.length > 0) {
      console.log('  📖 为您推荐的知识:');
      recommendations.forEach((rec, index) => {
        console.log(`  ${index + 1}. ${rec.item.title}`);
        console.log(`     💭 理由: ${rec.reason}`);
        console.log(`     📊 评分: ${rec.score.toFixed(2)}`);
        console.log('');
      });
    }

    console.log('🔍 智能搜索演示完成\n');
  }

  // ==================== 输入法集成演示 ====================

  private async demonstrateInputMethodIntegration(): Promise<void> {
    console.log('⌨️  === 输入法集成演示 ===');

    // 模拟不同的输入上下文
    const inputContexts: InputContext[] = [
      {
        currentText: 'TypeScript',
        precedingText: '我正在学习',
        followingText: '',
        cursorPosition: 12,
        application: 'VSCode',
        documentType: 'code',
        windowTitle: 'app.ts',
        timestamp: new Date(),
        sessionId: 'session_001'
      },
      {
        currentText: '机器学习',
        precedingText: '今天研究了',
        followingText: '的基础概念',
        cursorPosition: 6,
        application: 'Notepad',
        documentType: 'note',
        windowTitle: '学习笔记.md',
        timestamp: new Date(),
        sessionId: 'session_002'
      },
      {
        currentText: 'React Hooks',
        precedingText: '我想了解',
        followingText: '的用法',
        cursorPosition: 12,
        application: 'WebStorm',
        documentType: 'code',
        windowTitle: 'Component.jsx',
        timestamp: new Date(),
        sessionId: 'session_003'
      }
    ];

    // 1. 知识提示演示
    console.log('\n💡 知识提示演示...');
    for (const context of inputContexts) {
      console.log(`\n📝 输入上下文: "${context.currentText}" (${context.application})`);
      
      const tips = await this.tipEngine.generateTips(context);
      
      if (tips.length === 0) {
        console.log('  ❌ 暂无相关提示');
        continue;
      }

      console.log(`  💡 生成 ${tips.length} 个提示:`);
      tips.forEach((tip, index) => {
        console.log(`  ${index + 1}. [${tip.type}] ${tip.title}`);
        console.log(`     📄 内容: ${tip.content.substring(0, 100)}...`);
        console.log(`     📊 相关性: ${tip.relevance.toFixed(2)}`);
        console.log(`     🎯 置信度: ${tip.confidence.toFixed(2)}`);
        console.log(`     ⚡ 优先级: ${tip.priority}`);
        
        if (tip.actions.length > 0) {
          console.log(`     🛠️  可用操作: ${tip.actions.map(a => a.label).join(', ')}`);
        }
        console.log('');
      });
    }

    // 2. 智能补全演示
    console.log('\n🔤 智能补全演示...');
    const completionContexts = [
      {
        ...inputContexts[0],
        currentText: 'Type'
      },
      {
        ...inputContexts[1],
        currentText: '机器'
      },
      {
        ...inputContexts[2],
        currentText: 'Reac'
      }
    ];

    for (const context of completionContexts) {
      console.log(`\n🔤 补全上下文: "${context.currentText}"`);
      
      const suggestions = await this.completionEngine.generateCompletions(
        context.currentText,
        context
      );
      
      if (suggestions.length === 0) {
        console.log('  ❌ 暂无补全建议');
        continue;
      }

      console.log(`  💡 生成 ${suggestions.length} 个补全建议:`);
      suggestions.forEach((suggestion, index) => {
        console.log(`  ${index + 1}. "${suggestion.text}"`);
        console.log(`     📊 类型: ${suggestion.type}`);
        console.log(`     📦 来源: ${suggestion.source}`);
        console.log(`     🎯 置信度: ${suggestion.confidence.toFixed(2)}`);
        
        if (suggestion.description) {
          console.log(`     📝 描述: ${suggestion.description}`);
        }
        console.log('');
      });
    }

    // 3. 上下文推荐演示
    console.log('\n🎯 上下文推荐演示...');
    for (const context of inputContexts) {
      console.log(`\n🎯 推荐上下文: "${context.currentText}" (${context.documentType})`);
      
      const recommendations = await this.recommendationEngine.recommend(context);
      
      if (recommendations.length === 0) {
        console.log('  ❌ 暂无相关推荐');
        continue;
      }

      console.log(`  📖 生成 ${recommendations.length} 个推荐:`);
      recommendations.forEach((rec, index) => {
        console.log(`  ${index + 1}. ${rec.title}`);
        console.log(`     📄 摘要: ${rec.summary.substring(0, 100)}...`);
        console.log(`     📊 相关性: ${rec.relevanceScore.toFixed(2)}`);
        console.log(`     🏷️  类型: ${rec.recommendationType}`);
        console.log(`     🎯 置信度: ${rec.confidence.toFixed(2)}`);
        console.log(`     🛠️  推荐操作: ${rec.action.label} - ${rec.action.description}`);
        console.log('');
      });
    }

    console.log('⌨️  输入法集成演示完成\n');
  }

  // ==================== 学习辅助演示 ====================

  private async demonstrateLearningAssistance(): Promise<void> {
    console.log('🎓 === 学习辅助演示 ===');

    const learningContexts = [
      {
        goals: ['掌握TypeScript基础', '理解React Hooks'],
        currentTopic: 'TypeScript',
        difficulty: 'beginner',
        timeAvailable: 30
      },
      {
        goals: ['深入学习机器学习'],
        currentTopic: '机器学习',
        difficulty: 'intermediate',
        timeAvailable: 60
      },
      {
        goals: ['提升项目管理技能'],
        currentTopic: '项目管理',
        difficulty: 'advanced',
        timeAvailable: 45
      }
    ];

    for (let i = 0; i < learningContexts.length; i++) {
      const context = learningContexts[i];
      const userId = `demo_user_${i + 1}`;
      
      console.log(`\n👤 学习者: ${userId}`);
      console.log(`🎯 学习目标: ${context.goals.join(', ')}`);
      console.log(`📚 当前主题: ${context.currentTopic}`);
      console.log(`📊 难度级别: ${context.difficulty}`);
      console.log(`⏰ 可用时间: ${context.timeAvailable} 分钟`);

      const assistance = await this.learningEngine.assistLearning(userId, context);

      // 1. 复习计划
      console.log('\n📅 复习计划:');
      if (assistance.reviewPlan.items.length > 0) {
        assistance.reviewPlan.items.forEach((item, index) => {
          console.log(`  ${index + 1}. ${item.itemTitle}`);
          console.log(`     📦 复习类型: ${item.reviewType}`);
          console.log(`     ⏰ 间隔: ${item.interval} 天`);
          console.log(`     📊 难度: ${item.difficulty.toFixed(2)}`);
        });
      } else {
        console.log('  📝 暂无复习计划，建议先添加学习内容');
      }

      // 2. 记忆强化
      console.log('\n🧠 记忆强化:');
      console.log(`  🛠️  推荐技术: ${assistance.memoryEnhancement.techniques.join(', ')}`);
      console.log(`  💡 学习建议: ${assistance.memoryEnhancement.tips.join(', ')}`);
      
      if (assistance.memoryEnhancement.exercises.length > 0) {
        console.log('  🏃‍♂️ 记忆练习:');
        assistance.memoryEnhancement.exercises.forEach((exercise, index) => {
          console.log(`    ${index + 1}. ${exercise.description} (${exercise.type})`);
        });
      }

      // 3. 学习路径
      console.log('\n🛤️  学习路径:');
      if (assistance.learningPath.steps.length > 0) {
        console.log(`  📍 当前步骤: ${assistance.learningPath.currentStep + 1}/${assistance.learningPath.steps.length}`);
        console.log(`  ⏱️  预计时长: ${assistance.learningPath.estimatedDuration} 分钟`);
        
        assistance.learningPath.steps.forEach((step, index) => {
          const status = index === assistance.learningPath.currentStep ? '📍 当前' : 
                        index < assistance.learningPath.currentStep ? '✅ 已完成' : '⏳ 待完成';
          console.log(`  ${index + 1}. ${status} ${step.title}`);
          console.log(`     📝 ${step.description}`);
          console.log(`     🏷️  类型: ${step.type}`);
          console.log(`     ⏰ 预计: ${step.estimatedTime} 分钟`);
        });
      } else {
        console.log('  🗺️  暂无学习路径，建议先设定学习目标');
      }

      // 4. 学习建议
      console.log('\n💡 学习建议:');
      if (assistance.suggestions.length > 0) {
        assistance.suggestions.forEach((suggestion, index) => {
          const priorityIcon = suggestion.priority === 'high' ? '🔴' : 
                              suggestion.priority === 'medium' ? '🟡' : '🟢';
          console.log(`  ${index + 1}. ${priorityIcon} [${suggestion.type}] ${suggestion.title}`);
          console.log(`     📝 ${suggestion.description}`);
          console.log(`     🛠️  建议操作: ${suggestion.action.type} - ${suggestion.action.target}`);
        });
      } else {
        console.log('  📝 暂无特定建议，继续保持学习');
      }

      // 5. 学习状态
      console.log('\n📊 学习状态:');
      const state = assistance.learningState;
      console.log(`  📈 学习进度: ${(state.learningProgress * 100).toFixed(1)}%`);
      console.log(`  ✅ 已掌握: ${state.masteredItems.length} 项`);
      console.log(`  📚 学习中: ${state.learningItems.length} 项`);
      console.log(`  ⚠️  困难项: ${state.difficultItems.length} 项`);
      
      if (state.strengths.length > 0) {
        console.log(`  💪 优势领域: ${state.strengths.join(', ')}`);
      }
      if (state.weaknesses.length > 0) {
        console.log(`  🔧 需要改进: ${state.weaknesses.join(', ')}`);
      }
    }

    console.log('\n🎓 学习辅助演示完成\n');
  }

  // ==================== 系统统计演示 ====================

  private async demonstrateSystemStatistics(): Promise<void> {
    console.log('📊 === 系统统计演示 ===');

    const stats = await this.knowledgeBase.getStatistics();

    console.log('\n📈 知识库概览:');
    console.log(`  📚 总条目数: ${stats.totalItems}`);
    console.log(`  📂 总分类数: ${stats.totalCategories}`);
    console.log(`  🏷️  总标签数: ${stats.totalTags}`);
    console.log(`  ⭐ 平均质量: ${stats.averageQuality.toFixed(2)}`);
    console.log(`  🏆 最常用分类: ${stats.mostUsedCategory}`);

    console.log('\n🔥 热门标签:');
    stats.topTags.forEach((tag, index) => {
      console.log(`  ${index + 1}. ${tag}`);
    });

    console.log('\n🕐 最近添加:');
    stats.recentItems.forEach((item, index) => {
      console.log(`  ${index + 1}. ${item.title}`);
      console.log(`     📅 创建时间: ${item.createdAt.toLocaleString()}`);
      console.log(`     📂 分类: ${item.category}`);
      console.log(`     ⭐ 质量: ${item.quality.toFixed(2)}`);
    });

    // 分类统计
    console.log('\n📊 分类统计:');
    const categories = this.knowledgeBase.getCategories();
    categories.forEach(category => {
      const percentage = stats.totalItems > 0 ? 
        (category.itemCount / stats.totalItems * 100).toFixed(1) : '0.0';
      console.log(`  📂 ${category.name}: ${category.itemCount} 项 (${percentage}%)`);
    });

    // 质量分布
    console.log('\n⭐ 质量分布:');
    const allItems = this.knowledgeBase.getAllKnowledgeItems();
    const qualityRanges = [
      { range: '0.0-0.2', count: 0, label: '较差' },
      { range: '0.2-0.4', count: 0, label: '一般' },
      { range: '0.4-0.6', count: 0, label: '中等' },
      { range: '0.6-0.8', count: 0, label: '良好' },
      { range: '0.8-1.0', count: 0, label: '优秀' }
    ];

    allItems.forEach(item => {
      const quality = item.quality;
      if (quality <= 0.2) qualityRanges[0].count++;
      else if (quality <= 0.4) qualityRanges[1].count++;
      else if (quality <= 0.6) qualityRanges[2].count++;
      else if (quality <= 0.8) qualityRanges[3].count++;
      else qualityRanges[4].count++;
    });

    qualityRanges.forEach(range => {
      const percentage = stats.totalItems > 0 ? 
        (range.count / stats.totalItems * 100).toFixed(1) : '0.0';
      console.log(`  ${range.label} (${range.range}): ${range.count} 项 (${percentage}%)`);
    });

    // 时间分布
    console.log('\n📅 时间分布分析:');
    const now = new Date();
    const timeRanges = [
      { name: '今天', start: new Date(now.getFullYear(), now.getMonth(), now.getDate()), count: 0 },
      { name: '本周', start: new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000), count: 0 },
      { name: '本月', start: new Date(now.getFullYear(), now.getMonth(), 1), count: 0 },
      { name: '更早', start: new Date(0), count: 0 }
    ];

    allItems.forEach(item => {
      for (let i = 0; i < timeRanges.length; i++) {
        if (item.createdAt >= timeRanges[i].start) {
          timeRanges[i].count++;
          break;
        }
      }
    });

    timeRanges.forEach(range => {
      const percentage = stats.totalItems > 0 ? 
        (range.count / stats.totalItems * 100).toFixed(1) : '0.0';
      console.log(`  ${range.name}: ${range.count} 项 (${percentage}%)`);
    });

    console.log('\n📊 系统统计演示完成\n');
  }

  // ==================== 交互式演示 ====================

  async runInteractiveDemo(): Promise<void> {
    console.log('🎮 === 交互式演示 ===');
    console.log('您可以尝试以下功能:');
    console.log('1. 添加自定义知识');
    console.log('2. 搜索特定内容');
    console.log('3. 模拟输入法集成');
    console.log('4. 查看学习建议');
    console.log('5. 查看系统统计');
    console.log('输入 "exit" 退出演示\n');

    // 这里可以实现命令行交互
    // 由于是演示代码，我们只展示接口
    console.log('💡 交互式演示需要实现命令行界面');
    console.log('📝 可以参考 runCompleteDemo() 中的功能实现');
  }

  // ==================== 性能测试演示 ====================

  async runPerformanceTest(): Promise<void> {
    console.log('⚡ === 性能测试演示 ===');

    const testSizes = [10, 50, 100, 500];
    
    for (const size of testSizes) {
      console.log(`\n🧪 测试规模: ${size} 个知识条目`);
      
      // 1. 添加性能测试
      const startTime = Date.now();
      
      for (let i = 0; i < size; i++) {
        await this.knowledgeBase.addKnowledgeItem(
          `测试条目 ${i}`,
          `这是第 ${i} 个测试条目的内容。包含一些测试数据用于性能评估。`,
          ContentType.TEXT,
          'performance_test'
        );
      }
      
      const addTime = Date.now() - startTime;
      console.log(`  ⏱️  添加耗时: ${addTime}ms`);
      console.log(`  📊 平均每条: ${(addTime / size).toFixed(2)}ms`);

      // 2. 搜索性能测试
      const searchStartTime = Date.now();
      
      const searchResults = await this.knowledgeBase.searchKnowledge('测试', { limit: 20 });
      
      const searchTime = Date.now() - searchStartTime;
      console.log(`  🔍 搜索耗时: ${searchTime}ms`);
      console.log(`  📊 结果数量: ${searchResults.length}`);

      // 3. 推荐性能测试
      const recStartTime = Date.now();
      
      const recommendations = await this.knowledgeBase.getRecommendations('test_user', 10);
      
      const recTime = Date.now() - recStartTime;
      console.log(`  🎯 推荐耗时: ${recTime}ms`);
      console.log(`  📊 推荐数量: ${recommendations.length}`);

      // 清理测试数据
      const allItems = this.knowledgeBase.getAllKnowledgeItems();
      for (const item of allItems) {
        if (item.source === 'performance_test') {
          await this.knowledgeBase.deleteKnowledgeItem(item.id);
        }
      }
    }

    console.log('\n⚡ 性能测试演示完成\n');
  }
}

// ==================== 使用示例 ====================

export class PersonalKnowledgeSystemExample {
  private system: PersonalKnowledgeSystemDemo;

  constructor() {
    this.system = new PersonalKnowledgeSystemDemo();
  }

  // 基础使用示例
  async basicUsageExample(): Promise<void> {
    console.log('📚 === 基础使用示例 ===');

    // 1. 创建知识库管理器
    const knowledgeBase = new KnowledgeBaseManager();

    // 2. 添加知识条目
    const item = await knowledgeBase.addKnowledgeItem(
      'JavaScript闭包',
      `闭包是JavaScript中的一个重要概念。闭包是指函数可以访问其外部作用域中的变量，即使在外部函数执行完毕后。

特点：
1. 函数嵌套函数
2. 内部函数可以访问外部函数的变量
3. 外部函数的变量不会被垃圾回收

示例：
\`\`\`javascript
function outer(x) {
  return function inner(y) {
    return x + y;
  };
}

const add5 = outer(5);
console.log(add5(3)); // 8
\`\`\``,
      ContentType.CODE,
      '学习笔记'
    );

    console.log('✅ 添加知识条目成功');
    console.log(`📝 标题: ${item.title}`);
    console.log(`📂 分类: ${item.category}`);
    console.log(`🏷️  标签: [${item.tags.join(', ')}]`);

    // 3. 搜索知识
    const results = await knowledgeBase.searchKnowledge('闭包');
    console.log(`\n🔍 搜索结果: ${results.length} 个`);
    results.forEach(result => {
      console.log(`  • ${result.item.title} (评分: ${result.score.toFixed(2)})`);
    });

    // 4. 获取推荐
    const recommendations = await knowledgeBase.getRecommendations('user123');
    console.log(`\n🎯 推荐内容: ${recommendations.length} 个`);
    recommendations.forEach(rec => {
      console.log(`  • ${rec.item.title} (${rec.reason})`);
    });
  }

  // 输入法集成示例
  async inputMethodIntegrationExample(): Promise<void> {
    console.log('\n⌨️  === 输入法集成示例 ===');

    const knowledgeBase = new KnowledgeBaseManager();
    const tipEngine = new KnowledgeTipEngine(knowledgeBase, null);

    // 添加一些基础知识
    await knowledgeBase.addKnowledgeItem(
      'React Hooks',
      'React Hooks是React 16.8引入的新特性，允许在函数组件中使用状态等特性。',
      ContentType.TEXT,
      '技术文档'
    );

    // 模拟输入上下文
    const inputContext: InputContext = {
      currentText: 'React Hooks',
      precedingText: '我想了解',
      followingText: '的用法',
      cursorPosition: 12,
      application: 'VSCode',
      documentType: 'code',
      windowTitle: 'App.jsx',
      timestamp: new Date(),
      sessionId: 'demo_session'
    };

    // 生成知识提示
    const tips = await tipEngine.generateTips(inputContext);
    console.log(`💡 生成了 ${tips.length} 个知识提示:`);
    tips.forEach((tip, index) => {
      console.log(`  ${index + 1}. [${tip.type}] ${tip.title}`);
      console.log(`     📄 ${tip.content}`);
    });
  }

  // 学习辅助示例
  async learningAssistanceExample(): Promise<void> {
    console.log('\n🎓 === 学习辅助示例 ===');

    const knowledgeBase = new KnowledgeBaseManager();
    const learningEngine = new LearningAssistanceEngine(knowledgeBase);

    // 添加学习内容
    await knowledgeBase.addKnowledgeItem(
      'TypeScript基础',
      'TypeScript是JavaScript的超集，添加了静态类型检查等特性。',
      ContentType.TEXT,
      '学习资料'
    );

    const learningContext = {
      goals: ['掌握TypeScript基础'],
      currentTopic: 'TypeScript',
      difficulty: 'beginner' as const,
      timeAvailable: 30
    };

    // 获取学习辅助
    const assistance = await learningEngine.assistLearning('student123', learningContext);
    
    console.log('📅 复习计划:');
    assistance.reviewPlan.items.forEach((item, index) => {
      console.log(`  ${index + 1}. ${item.itemTitle}`);
    });

    console.log('\n💡 学习建议:');
    assistance.suggestions.forEach((suggestion, index) => {
      console.log(`  ${index + 1}. ${suggestion.title}: ${suggestion.description}`);
    });
  }

  // 完整工作流示例
  async completeWorkflowExample(): Promise<void> {
    console.log('\n🔄 === 完整工作流示例 ===');

    // 1. 初始化系统
    const system = new PersonalKnowledgeSystemDemo();

    // 2. 添加知识
    console.log('📚 添加知识...');
    await system.demonstrateKnowledgeCollection();

    // 3. 管理知识
    console.log('🔧 管理知识...');
    await system.demonstrateKnowledgeManagement();

    // 4. 搜索和推荐
    console.log('🔍 搜索和推荐...');
    await system.demonstrateIntelligentSearch();

    // 5. 输入法集成
    console.log('⌨️  输入法集成...');
    await system.demonstrateInputMethodIntegration();

    console.log('✅ 完整工作流演示完成');
  }
}

// ==================== 主程序入口 ====================

export async function main(): Promise<void> {
  console.log('🚀 个人知识库系统演示程序');
  console.log('=====================================\n');

  const demo = new PersonalKnowledgeSystemDemo();
  const example = new PersonalKnowledgeSystemExample();

  try {
    // 运行完整演示
    await demo.runCompleteDemo();

    // 运行基础使用示例
    console.log('\n📚 === 基础使用示例 ===');
    await example.basicUsageExample();

    // 运行性能测试
    console.log('\n⚡ === 性能测试 ===');
    await demo.runPerformanceTest();

    console.log('\n🎉 所有演示完成！');
    console.log('💡 您可以基于这些代码构建自己的个人知识库系统');

  } catch (error) {
    console.error('❌ 演示过程中出现错误:', error);
  }
}

// 如果直接运行此文件
if (require.main === module) {
  main().catch(console.error);
}

// ==================== 导出 ====================

export {
  PersonalKnowledgeSystemDemo,
  PersonalKnowledgeSystemExample,
  main
};