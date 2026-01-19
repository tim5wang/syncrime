// 输入法与知识库深度集成组件
// 实现实时知识提示、智能补全、上下文推荐等功能

// ==================== 输入法集成核心 ====================

export interface InputContext {
  currentText: string;
  precedingText: string;
  followingText: string;
  cursorPosition: number;
  application: string;
  documentType: string;
  windowTitle: string;
  timestamp: Date;
  userId?: string;
  sessionId: string;
}

export interface KnowledgeTip {
  id: string;
  type: TipType;
  title: string;
  content: string;
  relevance: number;
  confidence: number;
  priority: number;
  actions: TipAction[];
  metadata: Record<string, any>;
}

export enum TipType {
  DEFINITION = 'definition',
  EXAMPLE = 'example',
  RELATED_CONCEPT = 'related_concept',
  COMPLETION = 'completion',
  CORRECTION = 'correction',
  TRANSLATION = 'translation',
  FORMULA = 'formula',
  CODE_SNIPPET = 'code_snippet',
  REFERENCE = 'reference',
  REMINDER = 'reminder'
}

export interface TipAction {
  type: 'insert' | 'replace' | 'copy' | 'open' | 'learn';
  label: string;
  data: string;
  shortcut?: string;
}

export interface CompletionSuggestion {
  text: string;
  type: SuggestionType;
  source: SuggestionSource;
  confidence: number;
  description?: string;
  metadata?: Record<string, any>;
}

export enum SuggestionType {
  WORD = 'word',
  PHRASE = 'phrase',
  SENTENCE = 'sentence',
  CODE = 'code',
  FORMULA = 'formula',
  EMOJI = 'emoji',
  TEMPLATE = 'template'
}

export enum SuggestionSource {
  LANGUAGE_MODEL = 'language_model',
  KNOWLEDGE_BASE = 'knowledge_base',
  USER_HISTORY = 'user_history',
  DICTIONARY = 'dictionary',
  CUSTOM = 'custom'
}

// ==================== 知识提示引擎 ====================

export class KnowledgeTipEngine {
  private contextAnalyzer: InputContextAnalyzer;
  private knowledgeMatcher: KnowledgeMatcher;
  private tipRenderer: TipRenderer;
  private tipCache: Map<string, KnowledgeTip[]> = new Map();
  private readonly cacheTimeout = 60000; // 1分钟缓存

  constructor(
    private knowledgeBase: any,
    private userProfiler: any
  ) {
    this.contextAnalyzer = new InputContextAnalyzer();
    this.knowledgeMatcher = new KnowledgeMatcher(knowledgeBase);
    this.tipRenderer = new TipRenderer();
  }

  async generateTips(inputContext: InputContext): Promise<KnowledgeTip[]> {
    try {
      // 1. 检查缓存
      const cacheKey = this.generateCacheKey(inputContext);
      const cached = this.tipCache.get(cacheKey);
      if (cached && this.isCacheValid(cached)) {
        return cached;
      }

      // 2. 上下文分析
      const analysis = await this.contextAnalyzer.analyze(inputContext);
      
      // 3. 知识匹配
      const matchedKnowledge = await this.knowledgeMatcher.match(analysis);
      
      // 4. 提示生成
      const tips = await this.generateTipsFromKnowledge(matchedKnowledge, analysis, inputContext);
      
      // 5. 提示过滤和排序
      const filteredTips = this.filterAndSortTips(tips, analysis, inputContext);
      
      // 6. 缓存结果
      this.tipCache.set(cacheKey, filteredTips);
      
      return filteredTips;
    } catch (error) {
      console.error('Failed to generate knowledge tips:', error);
      return [];
    }
  }

  private async generateTipsFromKnowledge(
    knowledge: MatchedKnowledge[],
    analysis: ContextAnalysis,
    context: InputContext
  ): Promise<KnowledgeTip[]> {
    const tips: KnowledgeTip[] = [];

    for (const match of knowledge) {
      const tipTypes = this.selectTipTypes(match, analysis, context);
      
      for (const tipType of tipTypes) {
        const tip = await this.createTip(match, tipType, analysis, context);
        if (tip) {
          tips.push(tip);
        }
      }
    }

    return tips;
  }

  private selectTipTypes(
    match: MatchedKnowledge,
    analysis: ContextAnalysis,
    context: InputContext
  ): TipType[] {
    const types: TipType[] = [];

    // 基于匹配类型选择提示类型
    switch (match.type) {
      case 'keyword':
        types.push(TipType.DEFINITION, TipType.RELATED_CONCEPT);
        break;
      case 'entity':
        types.push(TipType.DEFINITION, TipType.REFERENCE);
        break;
      case 'concept':
        types.push(TipType.DEFINITION, TipType.EXAMPLE, TipType.RELATED_CONCEPT);
        break;
      case 'relation':
        types.push(TipType.RELATED_CONCEPT);
        break;
    }

    // 基于应用场景调整
    if (context.application === 'code') {
      types.push(TipType.CODE_SNIPPET);
    }
    if (context.documentType === 'academic') {
      types.push(TipType.REFERENCE, TipType.FORMULA);
    }

    return types;
  }

  private async createTip(
    match: MatchedKnowledge,
    tipType: TipType,
    analysis: ContextAnalysis,
    context: InputContext
  ): Promise<KnowledgeTip | null> {
    const tipId = this.generateTipId(match, tipType);
    
    switch (tipType) {
      case TipType.DEFINITION:
        return this.createDefinitionTip(match, analysis, context);
      case TipType.EXAMPLE:
        return this.createExampleTip(match, analysis, context);
      case TipType.RELATED_CONCEPT:
        return this.createRelatedConceptTip(match, analysis, context);
      case TipType.COMPLETION:
        return this.createCompletionTip(match, analysis, context);
      case TipType.CODE_SNIPPET:
        return this.createCodeSnippetTip(match, analysis, context);
      case TipType.REFERENCE:
        return this.createReferenceTip(match, analysis, context);
      default:
        return null;
    }
  }

  private createDefinitionTip(
    match: MatchedKnowledge,
    analysis: ContextAnalysis,
    context: InputContext
  ): KnowledgeTip {
    const keyword = match.data.text || match.data.name || match.data.word;
    const definition = this.extractDefinition(match);
    
    return {
      id: this.generateTipId(match, TipType.DEFINITION),
      type: TipType.DEFINITION,
      title: `定义：${keyword}`,
      content: definition,
      relevance: match.relevance,
      confidence: match.confidence,
      priority: this.calculatePriority(match, context),
      actions: [
        {
          type: 'insert',
          label: '插入定义',
          data: definition
        },
        {
          type: 'learn',
          label: '深入学习',
          data: match.id
        }
      ],
      metadata: {
        keyword,
        source: match.source,
        matchType: match.type
      }
    };
  }

  private createExampleTip(
    match: MatchedKnowledge,
    analysis: ContextAnalysis,
    context: InputContext
  ): KnowledgeTip {
    const concept = match.data.name || match.data.word;
    const examples = this.extractExamples(match);
    const example = examples[0] || '暂无示例';
    
    return {
      id: this.generateTipId(match, TipType.EXAMPLE),
      type: TipType.EXAMPLE,
      title: `示例：${concept}`,
      content: example,
      relevance: match.relevance * 0.9,
      confidence: match.confidence,
      priority: this.calculatePriority(match, context),
      actions: [
        {
          type: 'insert',
          label: '使用示例',
          data: example
        },
        {
          type: 'copy',
          label: '复制示例',
          data: example
        }
      ],
      metadata: {
        concept,
        examples,
        source: match.source
      }
    };
  }

  private createRelatedConceptTip(
    match: MatchedKnowledge,
    analysis: ContextAnalysis,
    context: InputContext
  ): KnowledgeTip {
    const concept = match.data.name || match.data.word;
    const related = this.extractRelatedConcepts(match);
    const relatedList = related.slice(0, 3).join('、');
    
    return {
      id: this.generateTipId(match, TipType.RELATED_CONCEPT),
      type: TipType.RELATED_CONCEPT,
      title: `相关概念：${concept}`,
      content: `相关概念：${relatedList}`,
      relevance: match.relevance * 0.8,
      confidence: match.confidence,
      priority: this.calculatePriority(match, context),
      actions: related.map(r => ({
        type: 'insert' as const,
        label: `插入：${r}`,
        data: r
      })),
      metadata: {
        concept,
        relatedConcepts: related,
        source: match.source
      }
    };
  }

  private createCompletionTip(
    match: MatchedKnowledge,
    analysis: ContextAnalysis,
    context: InputContext
  ): KnowledgeTip {
    const partialWord = analysis.currentWord;
    const completion = this.generateCompletion(match, partialWord);
    
    return {
      id: this.generateTipId(match, TipType.COMPLETION),
      type: TipType.COMPLETION,
      title: '智能补全',
      content: completion,
      relevance: match.relevance,
      confidence: match.confidence,
      priority: this.calculatePriority(match, context),
      actions: [
        {
          type: 'replace',
          label: '补全',
          data: completion
        }
      ],
      metadata: {
        partialWord,
        completion,
        source: match.source
      }
    };
  }

  private createCodeSnippetTip(
    match: MatchedKnowledge,
    analysis: ContextAnalysis,
    context: InputContext
  ): KnowledgeTip {
    const keyword = match.data.text || match.data.name;
    const snippet = this.generateCodeSnippet(match, context);
    
    return {
      id: this.generateTipId(match, TipType.CODE_SNIPPET),
      type: TipType.CODE_SNIPPET,
      title: `代码片段：${keyword}`,
      content: snippet,
      relevance: match.relevance * 0.95,
      confidence: match.confidence,
      priority: this.calculatePriority(match, context),
      actions: [
        {
          type: 'insert',
          label: '插入代码',
          data: snippet
        },
        {
          type: 'copy',
          label: '复制代码',
          data: snippet
        }
      ],
      metadata: {
        keyword,
        language: this.detectLanguage(context),
        source: match.source
      }
    };
  }

  private createReferenceTip(
    match: MatchedKnowledge,
    analysis: ContextAnalysis,
    context: InputContext
  ): KnowledgeTip {
    const entity = match.data.text;
    const reference = this.extractReference(match);
    
    return {
      id: this.generateTipId(match, TipType.REFERENCE),
      type: TipType.REFERENCE,
      title: `参考资料：${entity}`,
      content: reference,
      relevance: match.relevance * 0.85,
      confidence: match.confidence,
      priority: this.calculatePriority(match, context),
      actions: [
        {
          type: 'open',
          label: '查看详情',
          data: match.id
        },
        {
          type: 'copy',
          label: '复制引用',
          data: reference
        }
      ],
      metadata: {
        entity,
        reference,
        source: match.source
      }
    };
  }

  private extractDefinition(match: MatchedKnowledge): string {
    const data = match.data;
    
    if (data.definition) return data.definition;
    if (data.description) return data.description;
    if (data.content) return data.content.substring(0, 200) + '...';
    
    return `暂无定义信息：${data.name || data.text || data.word}`;
  }

  private extractExamples(match: MatchedKnowledge): string[] {
    const data = match.data;
    
    if (data.examples && Array.isArray(data.examples)) {
      return data.examples;
    }
    
    if (data.context && Array.isArray(data.context)) {
      return data.context.filter(ctx => ctx.length > 10);
    }
    
    return [];
  }

  private extractRelatedConcepts(match: MatchedKnowledge): string[] {
    const data = match.data;
    
    if (data.relatedConcepts && Array.isArray(data.relatedConcepts)) {
      return data.relatedConcepts;
    }
    
    if (data.relatedTags && Array.isArray(data.relatedTags)) {
      return data.relatedTags;
    }
    
    return [];
  }

  private generateCompletion(match: MatchedKnowledge, partialWord: string): string {
    const data = match.data;
    const candidates = [
      data.name,
      data.text,
      data.word,
      data.title
    ].filter(Boolean);
    
    const bestMatch = candidates.find(candidate => 
      candidate.toLowerCase().startsWith(partialWord.toLowerCase())
    );
    
    return bestMatch || candidates[0] || '';
  }

  private generateCodeSnippet(match: MatchedKnowledge, context: InputContext): string {
    const keyword = match.data.text || match.data.name;
    const language = this.detectLanguage(context);
    
    // 简化的代码片段生成
    const snippets: Record<string, Record<string, string>> = {
      javascript: {
        'function': `function ${keyword}() {\n  // 函数实现\n}`,
        'class': `class ${keyword} {\n  constructor() {\n    // 构造函数\n  }\n}`,
        'variable': `const ${keyword} = value;`
      },
      python: {
        'function': `def ${keyword}():\n    # 函数实现\n    pass`,
        'class': `class ${keyword}:\n    def __init__(self):\n        # 构造函数\n        pass`,
        'variable': `${keyword} = value`
      },
      typescript: {
        'interface': `interface ${keyword} {\n  // 接口定义\n}`,
        'type': `type ${keyword} = {\n  // 类型定义\n};`,
        'class': `class ${keyword} {\n  // 类定义\n}`
      }
    };
    
    return snippets[language]?.[keyword] || `// ${keyword} 代码片段`;
  }

  private extractReference(match: MatchedKnowledge): string {
    const data = match.data;
    
    if (data.url) return `链接：${data.url}`;
    if (data.reference) return data.reference;
    if (data.source) return `来源：${data.source}`;
    
    return `参考资料 #${match.id}`;
  }

  private detectLanguage(context: InputContext): string {
    const app = context.application.toLowerCase();
    const doc = context.documentType.toLowerCase();
    
    if (app.includes('python') || doc.includes('python')) return 'python';
    if (app.includes('typescript') || doc.includes('typescript')) return 'typescript';
    if (app.includes('javascript') || doc.includes('javascript')) return 'javascript';
    if (app.includes('java') || doc.includes('java')) return 'java';
    
    return 'javascript'; // 默认
  }

  private calculatePriority(match: MatchedKnowledge, context: InputContext): number {
    let priority = 1;
    
    // 基于相关性调整优先级
    priority += match.relevance * 2;
    
    // 基于置信度调整优先级
    priority += match.confidence;
    
    // 基于用户历史调整优先级
    if (context.userId) {
      priority += this.getUserPriorityBonus(match, context.userId);
    }
    
    return Math.min(10, Math.max(1, priority));
  }

  private getUserPriorityBonus(match: MatchedKnowledge, userId: string): number {
    // 简化的用户优先级计算
    return Math.random() * 0.5; // 实际应用中应基于用户历史数据
  }

  private filterAndSortTips(
    tips: KnowledgeTip[],
    analysis: ContextAnalysis,
    context: InputContext
  ): KnowledgeTip[] {
    // 过滤低质量提示
    const filtered = tips.filter(tip => 
      tip.confidence > 0.3 && 
      tip.relevance > 0.2 &&
      tip.priority > 1
    );

    // 去重
    const unique = this.deduplicateTips(filtered);

    // 排序
    return unique.sort((a, b) => {
      // 首先按优先级排序
      if (b.priority !== a.priority) {
        return b.priority - a.priority;
      }
      // 然后按相关性排序
      if (b.relevance !== a.relevance) {
        return b.relevance - a.relevance;
      }
      // 最后按置信度排序
      return b.confidence - a.confidence;
    }).slice(0, 5); // 最多返回5个提示
  }

  private deduplicateTips(tips: KnowledgeTip[]): KnowledgeTip[] {
    const seen = new Set<string>();
    return tips.filter(tip => {
      const key = `${tip.type}_${tip.title}`;
      if (seen.has(key)) {
        return false;
      }
      seen.add(key);
      return true;
    });
  }

  private generateCacheKey(context: InputContext): string {
    return `${context.currentText}_${context.application}_${context.documentType}`;
  }

  private isCacheValid(cached: KnowledgeTip[]): boolean {
    // 简化的缓存有效性检查
    return true; // 实际应用中应检查时间戳
  }

  private generateTipId(match: MatchedKnowledge, type: TipType): string {
    return `${match.id}_${type}_${Date.now()}`;
  }
}

// ==================== 智能补全引擎 ====================

export class IntelligentCompletionEngine {
  private contextModel: ContextualLanguageModel;
  private knowledgeIntegrator: KnowledgeIntegrator;
  private suggestionRanker: SuggestionRanker;
  private userHistory: UserInputHistory;

  constructor(
    private knowledgeBase: any,
    private userModel: any
  ) {
    this.contextModel = new ContextualLanguageModel(userModel);
    this.knowledgeIntegrator = new KnowledgeIntegrator(knowledgeBase);
    this.suggestionRanker = new SuggestionRanker();
    this.userHistory = new UserInputHistory();
  }

  async generateCompletions(
    partialInput: string,
    context: InputContext
  ): Promise<CompletionSuggestion[]> {
    try {
      // 1. 上下文理解
      const contextFeatures = await this.extractContextFeatures(partialInput, context);
      
      // 2. 语言模型预测
      const lmSuggestions = await this.contextModel.predict(partialInput, contextFeatures);
      
      // 3. 知识库增强
      const knowledgeSuggestions = await this.knowledgeIntegrator.enhance(
        lmSuggestions, 
        contextFeatures
      );
      
      // 4. 用户历史增强
      const historySuggestions = await this.getUserHistorySuggestions(partialInput, context);
      
      // 5. 合并所有建议
      const allSuggestions = [
        ...lmSuggestions,
        ...knowledgeSuggestions,
        ...historySuggestions
      ];
      
      // 6. 建议重排
      const rankedSuggestions = await this.suggestionRanker.rank(allSuggestions, context);
      
      // 7. 去重和过滤
      const finalSuggestions = this.deduplicateAndFilter(rankedSuggestions);
      
      return finalSuggestions.slice(0, 10);
    } catch (error) {
      console.error('Failed to generate completions:', error);
      return [];
    }
  }

  private async extractContextFeatures(
    input: string,
    context: InputContext
  ): Promise<ContextFeatures> {
    return {
      currentText: input,
      precedingText: context.precedingText,
      followingText: context.followingText,
      application: context.application,
      documentType: context.documentType,
      userIntent: await this.predictUserIntent(input, context),
      recentInputs: await this.userHistory.getRecentInputs(context.userId, 10),
      userProfile: await this.getUserProfile(context.userId),
      syntacticContext: this.analyzeSyntacticContext(input, context),
      semanticContext: await this.analyzeSemanticContext(input, context)
    };
  }

  private async predictUserIntent(input: string, context: InputContext): Promise<string> {
    // 简化的意图预测
    const inputLower = input.toLowerCase();
    
    if (inputLower.includes('如何') || inputLower.includes('怎么')) {
      return 'question';
    }
    if (inputLower.includes('定义') || inputLower.includes('是什么')) {
      return 'definition';
    }
    if (context.application === 'code') {
      return 'coding';
    }
    if (context.documentType === 'email') {
      return 'communication';
    }
    
    return 'general';
  }

  private analyzeSyntacticContext(input: string, context: InputContext): SyntacticContext {
    const lastChar = input.slice(-1);
    const precedingWords = context.precedingText.split(/\s+/).slice(-3);
    
    return {
      endsWithSpace: input.endsWith(' '),
      endsWithPunctuation: /[.!?，。！？]/.test(lastChar),
      precedingWords,
      sentencePosition: this.calculateSentencePosition(input, context),
      isStartOfLine: input.trim() === input,
      indentation: this.getIndentation(input)
    };
  }

  private async analyzeSemanticContext(input: string, context: InputContext): Promise<SemanticContext> {
    // 简化的语义分析
    const keywords = this.extractKeywords(input);
    const entities = this.extractEntities(input);
    
    return {
      keywords,
      entities,
      topic: await this.predictTopic(input, context),
      domain: this.inferDomain(context),
      complexity: this.assessComplexity(input)
    };
  }

  private calculateSentencePosition(input: string, context: InputContext): number {
    const fullText = context.precedingText + input;
    const sentences = fullText.split(/[.!?。！？]/);
    return sentences.length;
  }

  private getIndentation(input: string): string {
    const match = input.match(/^(\s*)/);
    return match ? match[1] : '';
  }

  private extractKeywords(text: string): string[] {
    // 简化的关键词提取
    return text
      .toLowerCase()
      .split(/\s+/)
      .filter(word => word.length > 2)
      .slice(0, 5);
  }

  private extractEntities(text: string): string[] {
    // 简化的实体提取
    const entities: string[] = [];
    
    // 提取邮箱
    const emails = text.match(/\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b/g);
    if (emails) entities.push(...emails);
    
    // 提取URL
    const urls = text.match(/https?:\/\/[^\s]+/g);
    if (urls) entities.push(...urls);
    
    return entities;
  }

  private async predictTopic(input: string, context: InputContext): Promise<string> {
    // 基于关键词预测主题
    const keywords = this.extractKeywords(input);
    
    if (keywords.some(k => ['技术', '编程', '代码', '开发'].includes(k))) {
      return 'technology';
    }
    if (keywords.some(k => ['学习', '教育', '课程', '知识'].includes(k))) {
      return 'education';
    }
    if (keywords.some(k => ['工作', '职业', '职场', '项目'].includes(k))) {
      return 'work';
    }
    
    return 'general';
  }

  private inferDomain(context: InputContext): string {
    const app = context.application.toLowerCase();
    
    if (app.includes('code') || app.includes('ide')) return 'programming';
    if (app.includes('word') || app.includes('document')) return 'writing';
    if (app.includes('excel') || app.includes('sheet')) return 'data';
    if (app.includes('browser') || app.includes('web')) return 'web';
    
    return 'general';
  }

  private assessComplexity(input: string): number {
    // 简化的复杂度评估
    const words = input.split(/\s+/);
    const avgWordLength = words.reduce((sum, word) => sum + word.length, 0) / words.length;
    const uniqueWords = new Set(words.map(w => w.toLowerCase())).size;
    
    return Math.min(1.0, (avgWordLength / 10 + uniqueWords / words.length) / 2);
  }

  private async getUserProfile(userId: string): Promise<UserProfile> {
    // 简化的用户画像
    return {
      id: userId,
      preferences: {
        language: 'zh-CN',
        domain: 'general',
        complexity: 'medium'
      },
      expertise: ['general'],
      recentTopics: []
    };
  }

  private async getUserHistorySuggestions(
    partialInput: string,
    context: InputContext
  ): Promise<CompletionSuggestion[]> {
    if (!context.userId) return [];
    
    const history = await this.userHistory.getMatchingInputs(
      context.userId,
      partialInput,
      5
    );
    
    return history.map(item => ({
      text: item.input,
      type: SuggestionType.PHRASE,
      source: SuggestionSource.USER_HISTORY,
      confidence: 0.8,
      description: '历史输入',
      metadata: {
        frequency: item.frequency,
        lastUsed: item.lastUsed
      }
    }));
  }

  private deduplicateAndFilter(suggestions: CompletionSuggestion[]): CompletionSuggestion[] {
    // 去重
    const unique = new Map<string, CompletionSuggestion>();
    
    suggestions.forEach(suggestion => {
      const key = suggestion.text.toLowerCase();
      if (!unique.has(key) || unique.get(key)!.confidence < suggestion.confidence) {
        unique.set(key, suggestion);
      }
    });
    
    // 过滤低质量建议
    return Array.from(unique.values()).filter(s => 
      s.confidence > 0.2 && s.text.length > 0
    );
  }
}

// ==================== 上下文推荐引擎 ====================

export class ContextualRecommendationEngine {
  private contextEncoder: ContextEncoder;
  private knowledgeRetriever: KnowledgeRetriever;
  private relevanceCalculator: RelevanceCalculator;

  constructor(private knowledgeBase: any) {
    this.contextEncoder = new ContextEncoder();
    this.knowledgeRetriever = new KnowledgeRetriever(knowledgeBase);
    this.relevanceCalculator = new RelevanceCalculator();
  }

  async recommend(context: InputContext): Promise<ContextualRecommendation[]> {
    try {
      // 1. 上下文编码
      const contextVector = await this.contextEncoder.encode(context);
      
      // 2. 候选知识检索
      const candidates = await this.knowledgeRetriever.retrieve(contextVector, 20);
      
      // 3. 相关性计算
      const scoredCandidates = await Promise.all(
        candidates.map(async candidate => ({
          ...candidate,
          relevanceScore: await this.relevanceCalculator.calculate(candidate, context)
        }))
      );
      
      // 4. 推荐生成
      const recommendations = this.generateRecommendations(scoredCandidates, context);
      
      // 5. 多样性优化
      const diversified = this.diversifyRecommendations(recommendations);
      
      return diversified.slice(0, 8);
    } catch (error) {
      console.error('Failed to generate contextual recommendations:', error);
      return [];
    }
  }

  private generateRecommendations(
    candidates: ScoredCandidate[],
    context: InputContext
  ): ContextualRecommendation[] {
    return candidates
      .filter(c => c.relevanceScore > 0.5)
      .sort((a, b) => b.relevanceScore - a.relevanceScore)
      .slice(0, 10)
      .map(candidate => this.createRecommendation(candidate, context));
  }

  private createRecommendation(
    candidate: ScoredCandidate,
    context: InputContext
  ): ContextualRecommendation {
    const recommendationType = this.determineRecommendationType(candidate, context);
    
    return {
      itemId: candidate.id,
      title: candidate.title,
      summary: this.generateSummary(candidate),
      relevanceScore: candidate.relevanceScore,
      recommendationType,
      action: this.suggestAction(candidate, context),
      confidence: candidate.relevanceScore,
      metadata: {
        category: candidate.category,
        tags: candidate.tags,
        quality: candidate.quality
      }
    };
  }

  private determineRecommendationType(
    candidate: ScoredCandidate,
    context: InputContext
  ): RecommendationType {
    const app = context.application.toLowerCase();
    
    if (app.includes('code') && candidate.category === '技术') {
      return RecommendationType.CODE_REFERENCE;
    }
    if (context.documentType === 'academic') {
      return RecommendationType.ACADEMIC_REFERENCE;
    }
    if (candidate.tags.includes('tutorial') || candidate.tags.includes('guide')) {
      return RecommendationType.TUTORIAL;
    }
    if (candidate.tags.includes('example') || candidate.tags.includes('sample')) {
      return RecommendationType.EXAMPLE;
    }
    
    return RecommendationType.GENERAL;
  }

  private generateSummary(candidate: ScoredCandidate): string {
    const content = candidate.content || candidate.description || '';
    return content.length > 100 ? content.substring(0, 100) + '...' : content;
  }

  private suggestAction(
    candidate: ScoredCandidate,
    context: InputContext
  ): RecommendationAction {
    const actions: RecommendationAction[] = [
      {
        type: 'view',
        label: '查看详情',
        description: '打开知识条目详情'
      },
      {
        type: 'insert',
        label: '插入内容',
        description: '将内容插入到当前位置'
      },
      {
        type: 'copy',
        label: '复制',
        description: '复制到剪贴板'
      }
    ];

    // 根据上下文调整推荐动作
    if (context.application === 'code') {
      actions.unshift({
        type: 'insert_code',
        label: '插入代码',
        description: '插入代码片段'
      });
    }

    return actions[0]; // 返回最相关的动作
  }

  private diversifyRecommendations(recommendations: ContextualRecommendation[]): ContextualRecommendation[] {
    // 简化的多样性优化
    const diversified: ContextualRecommendation[] = [];
    const usedCategories = new Set<string>();
    const usedTypes = new Set<RecommendationType>();

    for (const rec of recommendations) {
      if (!usedCategories.has(rec.metadata.category) || 
          !usedTypes.has(rec.recommendationType)) {
        diversified.push(rec);
        usedCategories.add(rec.metadata.category);
        usedTypes.add(rec.recommendationType);
      }
    }

    // 如果多样性过滤后数量太少，补充剩余的推荐
    if (diversified.length < 5) {
      for (const rec of recommendations) {
        if (!diversified.includes(rec)) {
          diversified.push(rec);
          if (diversified.length >= 8) break;
        }
      }
    }

    return diversified;
  }
}

// ==================== 学习辅助引擎 ====================

export class LearningAssistanceEngine {
  private spacedRepetition: SpacedRepetitionScheduler;
  private memoryModel: MemoryModel;
  private learningPathGenerator: LearningPathGenerator;

  constructor(private knowledgeBase: any) {
    this.spacedRepetition = new SpacedRepetitionScheduler();
    this.memoryModel = new MemoryModel();
    this.learningPathGenerator = new LearningPathGenerator(knowledgeBase);
  }

  async assistLearning(
    userId: string,
    context: LearningContext
  ): Promise<LearningAssistance> {
    try {
      // 1. 学习状态评估
      const learningState = await this.assessLearningState(userId, context);
      
      // 2. 复习计划生成
      const reviewPlan = await this.spacedRepetition.generatePlan(learningState);
      
      // 3. 记忆强化
      const memoryEnhancement = await this.memoryModel.enhance(learningState, context);
      
      // 4. 学习路径推荐
      const learningPath = await this.learningPathGenerator.generate(learningState, context);
      
      // 5. 学习建议生成
      const suggestions = await this.generateLearningSuggestions(learningState, context);
      
      return {
        reviewPlan,
        memoryEnhancement,
        learningPath,
        suggestions,
        learningState
      };
    } catch (error) {
      console.error('Failed to assist learning:', error);
      return this.createDefaultAssistance();
    }
  }

  private async assessLearningState(
    userId: string,
    context: LearningContext
  ): Promise<LearningState> {
    const userKnowledge = await this.getUserKnowledge(userId);
    const interactionHistory = await this.getInteractionHistory(userId);
    
    return {
      masteredItems: this.identifyMasteredItems(userKnowledge),
      learningItems: this.identifyLearningItems(userKnowledge),
      difficultItems: this.identifyDifficultItems(userKnowledge, interactionHistory),
      reviewSchedule: this.calculateReviewSchedule(userKnowledge, interactionHistory),
      learningProgress: this.calculateLearningProgress(userKnowledge),
      strengths: this.identifyStrengths(userKnowledge),
      weaknesses: this.identifyWeaknesses(userKnowledge),
      goals: context.goals || []
    };
  }

  private async generateLearningSuggestions(
    state: LearningState,
    context: LearningContext
  ): Promise<LearningSuggestion[]> {
    const suggestions: LearningSuggestion[] = [];

    // 基于薄弱环节的建议
    if (state.weaknesses.length > 0) {
      suggestions.push({
        type: 'focus_area',
        title: '重点关注',
        description: `建议重点学习：${state.weaknesses.slice(0, 3).join('、')}`,
        priority: 'high',
        action: {
          type: 'study',
          target: state.weaknesses[0]
        }
      });
    }

    // 基于学习进度的建议
    if (state.learningProgress < 0.5) {
      suggestions.push({
        type: 'foundation',
        title: '巩固基础',
        description: '建议先巩固基础知识再进阶学习',
        priority: 'medium',
        action: {
          type: 'review',
          target: 'foundation'
        }
      });
    }

    // 基于复习计划的建议
    if (state.reviewSchedule.length > 0) {
      const nextReview = state.reviewSchedule[0];
      suggestions.push({
        type: 'review',
        title: '及时复习',
        description: `建议复习：${nextReview.itemTitle}`,
        priority: 'high',
        action: {
          type: 'review',
          target: nextReview.itemId
        }
      });
    }

    return suggestions;
  }

  private createDefaultAssistance(): LearningAssistance {
    return {
      reviewPlan: {
        items: [],
        schedule: [],
        nextReview: null
      },
      memoryEnhancement: {
        techniques: ['spaced_repetition', 'active_recall'],
        tips: ['定期复习', '主动回忆']
      },
      learningPath: {
        steps: [],
        currentStep: 0,
        estimatedDuration: 0
      },
      suggestions: [],
      learningState: {
        masteredItems: [],
        learningItems: [],
        difficultItems: [],
        reviewSchedule: [],
        learningProgress: 0,
        strengths: [],
        weaknesses: [],
        goals: []
      }
    };
  }

  // 私有辅助方法
  private async getUserKnowledge(userId: string): Promise<any[]> {
    // 获取用户知识数据
    return [];
  }

  private async getInteractionHistory(userId: string): Promise<any[]> {
    // 获取用户交互历史
    return [];
  }

  private identifyMasteredItems(knowledge: any[]): string[] {
    // 识别已掌握的项目
    return [];
  }

  private identifyLearningItems(knowledge: any[]): string[] {
    // 识别学习中的项目
    return [];
  }

  private identifyDifficultItems(knowledge: any[], history: any[]): string[] {
    // 识别困难项目
    return [];
  }

  private calculateReviewSchedule(knowledge: any[], history: any[]): any[] {
    // 计算复习计划
    return [];
  }

  private calculateLearningProgress(knowledge: any[]): number {
    // 计算学习进度
    return 0;
  }

  private identifyStrengths(knowledge: any[]): string[] {
    // 识别优势领域
    return [];
  }

  private identifyWeaknesses(knowledge: any[]): string[] {
    // 识别薄弱环节
    return [];
  }
}

// ==================== 辅助类和接口 ====================

export class InputContextAnalyzer {
  async analyze(context: InputContext): Promise<ContextAnalysis> {
    return {
      currentWord: this.extractCurrentWord(context),
      sentence: this.extractCurrentSentence(context),
      language: this.detectLanguage(context),
      topic: await this.predictTopic(context),
      intent: await this.predictIntent(context),
      entities: this.extractEntities(context),
      keywords: this.extractKeywords(context)
    };
  }

  private extractCurrentWord(context: InputContext): string {
    const text = context.currentText;
    const words = text.split(/\s+/);
    return words[words.length - 1] || '';
  }

  private extractCurrentSentence(context: InputContext): string {
    const fullText = context.precedingText + context.currentText;
    const sentences = fullText.split(/[.!?。！？]/);
    return sentences[sentences.length - 1] || '';
  }

  private detectLanguage(context: InputContext): string {
    const text = context.currentText;
    const chineseChars = (text.match(/[\u4e00-\u9fa5]/g) || []).length;
    const englishChars = (text.match(/[a-zA-Z]/g) || []).length;
    
    return chineseChars > englishChars ? 'zh' : 'en';
  }

  private async predictTopic(context: InputContext): Promise<string> {
    // 简化的主题预测
    return 'general';
  }

  private async predictIntent(context: InputContext): Promise<string> {
    // 简化的意图预测
    return 'general';
  }

  private extractEntities(context: InputContext): string[] {
    // 简化的实体提取
    return [];
  }

  private extractKeywords(context: InputContext): string[] {
    // 简化的关键词提取
    return context.currentText.split(/\s+/).filter(w => w.length > 2);
  }
}

export class KnowledgeMatcher {
  constructor(private knowledgeBase: any) {}

  async match(analysis: ContextAnalysis): Promise<MatchedKnowledge[]> {
    const matches: MatchedKnowledge[] = [];
    
    // 基于关键词匹配
    for (const keyword of analysis.keywords) {
      const items = await this.knowledgeBase.searchKnowledge(keyword);
      items.forEach(item => {
        matches.push({
          id: item.id,
          type: 'keyword',
          data: { word: keyword },
          relevance: this.calculateRelevance(keyword, item),
          confidence: 0.8,
          source: 'knowledge_base'
        });
      });
    }

    return matches.sort((a, b) => b.relevance - a.relevance).slice(0, 10);
  }

  private calculateRelevance(keyword: string, item: any): number {
    // 简化的相关性计算
    const titleMatch = item.title.toLowerCase().includes(keyword.toLowerCase());
    const contentMatch = item.content.toLowerCase().includes(keyword.toLowerCase());
    
    return titleMatch ? 0.9 : contentMatch ? 0.7 : 0.5;
  }
}

// 更多辅助类...
export class TipRenderer {
  render(tip: KnowledgeTip): string {
    return `${tip.title}: ${tip.content}`;
  }
}

export class ContextualLanguageModel {
  constructor(private userModel: any) {}
  
  async predict(input: string, features: ContextFeatures): Promise<CompletionSuggestion[]> {
    // 简化的语言模型预测
    return [{
      text: input + '预测',
      type: SuggestionType.WORD,
      source: SuggestionSource.LANGUAGE_MODEL,
      confidence: 0.7
    }];
  }
}

export class KnowledgeIntegrator {
  constructor(private knowledgeBase: any) {}
  
  async enhance(suggestions: CompletionSuggestion[], features: ContextFeatures): Promise<CompletionSuggestion[]> {
    // 知识库增强
    return suggestions;
  }
}

export class SuggestionRanker {
  async rank(suggestions: CompletionSuggestion[], context: InputContext): Promise<CompletionSuggestion[]> {
    // 建议排序
    return suggestions.sort((a, b) => b.confidence - a.confidence);
  }
}

export class UserInputHistory {
  async getRecentInputs(userId: string, limit: number): Promise<string[]> {
    // 获取最近输入
    return [];
  }
  
  async getMatchingInputs(userId: string, pattern: string, limit: number): Promise<any[]> {
    // 获取匹配的历史输入
    return [];
  }
}

// ==================== 类型定义 ====================

export interface ContextAnalysis {
  currentWord: string;
  sentence: string;
  language: string;
  topic: string;
  intent: string;
  entities: string[];
  keywords: string[];
}

export interface MatchedKnowledge {
  id: string;
  type: string;
  data: any;
  relevance: number;
  confidence: number;
  source: string;
}

export interface ContextFeatures {
  currentText: string;
  precedingText: string;
  followingText: string;
  application: string;
  documentType: string;
  userIntent: string;
  recentInputs: string[];
  userProfile: UserProfile;
  syntacticContext: SyntacticContext;
  semanticContext: SemanticContext;
}

export interface UserProfile {
  id: string;
  preferences: UserPreferences;
  expertise: string[];
  recentTopics: string[];
}

export interface UserPreferences {
  language: string;
  domain: string;
  complexity: string;
}

export interface SyntacticContext {
  endsWithSpace: boolean;
  endsWithPunctuation: boolean;
  precedingWords: string[];
  sentencePosition: number;
  isStartOfLine: boolean;
  indentation: string;
}

export interface SemanticContext {
  keywords: string[];
  entities: string[];
  topic: string;
  domain: string;
  complexity: number;
}

export interface ScoredCandidate {
  id: string;
  title: string;
  content: string;
  description: string;
  category: string;
  tags: string[];
  quality: number;
  relevanceScore: number;
}

export interface ContextualRecommendation {
  itemId: string;
  title: string;
  summary: string;
  relevanceScore: number;
  recommendationType: RecommendationType;
  action: RecommendationAction;
  confidence: number;
  metadata: Record<string, any>;
}

export enum RecommendationType {
  GENERAL = 'general',
  CODE_REFERENCE = 'code_reference',
  ACADEMIC_REFERENCE = 'academic_reference',
  TUTORIAL = 'tutorial',
  EXAMPLE = 'example',
  QUICK_REFERENCE = 'quick_reference'
}

export interface RecommendationAction {
  type: 'view' | 'insert' | 'copy' | 'insert_code';
  label: string;
  description: string;
}

export interface LearningContext {
  goals?: string[];
  currentTopic?: string;
  difficulty?: string;
  timeAvailable?: number;
}

export interface LearningAssistance {
  reviewPlan: ReviewPlan;
  memoryEnhancement: MemoryEnhancement;
  learningPath: LearningPath;
  suggestions: LearningSuggestion[];
  learningState: LearningState;
}

export interface ReviewPlan {
  items: ReviewItem[];
  schedule: ReviewSchedule[];
  nextReview: Date | null;
}

export interface ReviewItem {
  itemId: string;
  itemTitle: string;
  reviewType: string;
  interval: number;
  difficulty: number;
}

export interface ReviewSchedule {
  itemId: string;
  scheduledDate: Date;
  reviewType: string;
}

export interface MemoryEnhancement {
  techniques: string[];
  tips: string[];
  exercises: MemoryExercise[];
}

export interface MemoryExercise {
  type: string;
  description: string;
  difficulty: number;
}

export interface LearningPath {
  steps: LearningStep[];
  currentStep: number;
  estimatedDuration: number;
}

export interface LearningStep {
  id: string;
  title: string;
  description: string;
  type: string;
  resources: string[];
  estimatedTime: number;
}

export interface LearningSuggestion {
  type: string;
  title: string;
  description: string;
  priority: 'low' | 'medium' | 'high';
  action: LearningAction;
}

export interface LearningAction {
  type: string;
  target: string;
}

export interface LearningState {
  masteredItems: string[];
  learningItems: string[];
  difficultItems: string[];
  reviewSchedule: ReviewSchedule[];
  learningProgress: number;
  strengths: string[];
  weaknesses: string[];
  goals: string[];
}

// 辅助类
export class ContextEncoder {
  async encode(context: InputContext): Promise<number[]> {
    // 简化的上下文编码
    return [0, 1, 2];
  }
}

export class KnowledgeRetriever {
  constructor(private knowledgeBase: any) {}
  
  async retrieve(vector: number[], limit: number): Promise<ScoredCandidate[]> {
    // 简化的知识检索
    return [];
  }
}

export class RelevanceCalculator {
  async calculate(candidate: ScoredCandidate, context: InputContext): Promise<number> {
    // 简化的相关性计算
    return Math.random();
  }
}

export class SpacedRepetitionScheduler {
  async generatePlan(state: LearningState): Promise<ReviewPlan> {
    // 简化的间隔重复计划生成
    return {
      items: [],
      schedule: [],
      nextReview: null
    };
  }
}

export class MemoryModel {
  async enhance(state: LearningState, context: LearningContext): Promise<MemoryEnhancement> {
    // 简化的记忆模型增强
    return {
      techniques: ['spaced_repetition'],
      tips: ['定期复习'],
      exercises: []
    };
  }
}

export class LearningPathGenerator {
  constructor(private knowledgeBase: any) {}
  
  async generate(state: LearningState, context: LearningContext): Promise<LearningPath> {
    // 简化的学习路径生成
    return {
      steps: [],
      currentStep: 0,
      estimatedDuration: 0
    };
  }
}

// ==================== 导出 ====================

export {
  KnowledgeTipEngine,
  IntelligentCompletionEngine,
  ContextualRecommendationEngine,
  LearningAssistanceEngine,
  InputContextAnalyzer,
  KnowledgeMatcher
};