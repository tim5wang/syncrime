// 个人知识库系统核心实现
// 基于TypeScript的完整知识库管理系统

// ==================== 基础数据模型 ====================

export enum ContentType {
  TEXT = 'text',
  IMAGE = 'image',
  AUDIO = 'audio',
  VIDEO = 'video',
  DOCUMENT = 'document',
  CODE = 'code',
  LINK = 'link'
}

export enum Priority {
  LOW = 0,
  NORMAL = 1,
  HIGH = 2,
  URGENT = 3
}

export enum ItemStatus {
  DRAFT = 'draft',
  ACTIVE = 'active',
  ARCHIVED = 'archived',
  DELETED = 'deleted'
}

export interface KnowledgeItem {
  id: string;
  title: string;
  content: string;
  contentType: ContentType;
  source: string;
  category: string;
  tags: string[];
  priority: Priority;
  quality: number;
  createdAt: Date;
  updatedAt: Date;
  lastAccessed: Date;
  accessCount: number;
  relatedItems: string[];
  references: Reference[];
  metadata: Record<string, any>;
  version: number;
  status: ItemStatus;
}

export interface Reference {
  id: string;
  type: 'internal' | 'external' | 'citation';
  source: string;
  title?: string;
  url?: string;
  description?: string;
}

export interface Category {
  id: string;
  name: string;
  parentId?: string;
  level: number;
  path: string;
  description?: string;
  icon?: string;
  color?: string;
  itemCount: number;
  createdAt: Date;
  updatedAt: Date;
}

export interface Tag {
  id: string;
  name: string;
  type: string;
  category?: string;
  weight: number;
  color?: string;
  description?: string;
  usageCount: number;
  relatedTags: string[];
  createdAt: Date;
}

export interface KnowledgeRelation {
  id: string;
  sourceId: string;
  targetId: string;
  type: RelationType;
  strength: number;
  bidirectional: boolean;
  metadata?: Record<string, any>;
  createdAt: Date;
}

export enum RelationType {
  SIMILAR = 'similar',
  REFERENCE = 'reference',
  DEPENDENCY = 'dependency',
  SEQUENCE = 'sequence',
  CAUSATION = 'causation',
  OPPOSITION = 'opposition',
  EXAMPLE = 'example',
  EXTENSION = 'extension'
}

// ==================== 知识提取器 ====================

export class TextKnowledgeExtractor {
  private keywordExtractor: KeywordExtractor;
  private entityRecognizer: EntityRecognizer;
  private conceptExtractor: ConceptExtractor;
  private relationExtractor: RelationExtractor;

  constructor() {
    this.keywordExtractor = new KeywordExtractor();
    this.entityRecognizer = new EntityRecognizer();
    this.conceptExtractor = new ConceptExtractor();
    this.relationExtractor = new RelationExtractor();
  }

  async extract(rawText: string, context: ExtractionContext): Promise<ExtractedKnowledge[]> {
    const extracted: ExtractedKnowledge[] = [];

    try {
      // 并行提取各种知识元素
      const [keywords, entities, concepts, relations] = await Promise.all([
        this.keywordExtractor.extract(rawText),
        this.entityRecognizer.extract(rawText),
        this.conceptExtractor.extract(rawText),
        this.relationExtractor.extract(rawText)
      ]);

      // 整合提取结果
      if (keywords.length > 0) {
        extracted.push({
          type: 'keywords',
          data: keywords,
          confidence: this.calculateKeywordConfidence(keywords),
          source: rawText
        });
      }

      if (entities.length > 0) {
        extracted.push({
          type: 'entities',
          data: entities,
          confidence: this.calculateEntityConfidence(entities),
          source: rawText
        });
      }

      if (concepts.length > 0) {
        extracted.push({
          type: 'concepts',
          data: concepts,
          confidence: this.calculateConceptConfidence(concepts),
          source: rawText
        });
      }

      if (relations.length > 0) {
        extracted.push({
          type: 'relations',
          data: relations,
          confidence: this.calculateRelationConfidence(relations),
          source: rawText
        });
      }

    } catch (error) {
      console.error('Knowledge extraction failed:', error);
    }

    return extracted;
  }

  private calculateKeywordConfidence(keywords: Keyword[]): number {
    if (keywords.length === 0) return 0;
    const avgScore = keywords.reduce((sum, kw) => sum + kw.score, 0) / keywords.length;
    return Math.min(avgScore, 1.0);
  }

  private calculateEntityConfidence(entities: Entity[]): number {
    if (entities.length === 0) return 0;
    const avgScore = entities.reduce((sum, e) => sum + e.confidence, 0) / entities.length;
    return Math.min(avgScore, 1.0);
  }

  private calculateConceptConfidence(concepts: Concept[]): number {
    if (concepts.length === 0) return 0;
    const avgScore = concepts.reduce((sum, c) => sum + c.relevance, 0) / concepts.length;
    return Math.min(avgScore, 1.0);
  }

  private calculateRelationConfidence(relations: Relation[]): number {
    if (relations.length === 0) return 0;
    const avgScore = relations.reduce((sum, r) => sum + r.strength, 0) / relations.length;
    return Math.min(avgScore, 1.0);
  }
}

// 关键词提取器
export class KeywordExtractor {
  private readonly minKeywordLength = 2;
  private readonly maxKeywords = 20;

  async extract(text: string): Promise<Keyword[]> {
    const keywords: Keyword[] = [];
    
    // 1. 文本预处理
    const cleanText = this.preprocessText(text);
    
    // 2. 分词
    const tokens = this.tokenize(cleanText);
    
    // 3. 计算TF-IDF
    const tfidfScores = this.calculateTFIDF(tokens);
    
    // 4. TextRank算法
    const textRankScores = this.calculateTextRank(tokens);
    
    // 5. 合并和排序
    const combinedScores = this.combineScores(tfidfScores, textRankScores);
    
    // 6. 过滤和选择
    for (const [token, score] of combinedScores) {
      if (token.length >= this.minKeywordLength && score > 0.1) {
        keywords.push({
          word: token,
          score: score,
          frequency: this.getFrequency(token, tokens),
          positions: this.getPositions(token, text)
        });
      }
    }

    return keywords
      .sort((a, b) => b.score - a.score)
      .slice(0, this.maxKeywords);
  }

  private preprocessText(text: string): string {
    return text
      .toLowerCase()
      .replace(/[^\u4e00-\u9fa5a-zA-Z0-9\s]/g, ' ')
      .replace(/\s+/g, ' ')
      .trim();
  }

  private tokenize(text: string): string[] {
    // 简单的中英文分词实现
    // 实际应用中应使用专业的分词库如jieba
    const tokens: string[] = [];
    
    // 中文分词（简化版）
    const chineseRegex = /[\u4e00-\u9fa5]+/g;
    const chineseMatches = text.match(chineseRegex) || [];
    chineseMatches.forEach(match => {
      // 简单的2-gram分词
      for (let i = 0; i < match.length - 1; i++) {
        tokens.push(match.substr(i, 2));
      }
    });

    // 英文分词
    const englishRegex = /[a-zA-Z]+/g;
    const englishMatches = text.match(englishRegex) || [];
    tokens.push(...englishMatches);

    return tokens;
  }

  private calculateTFIDF(tokens: string[]): Map<string, number> {
    const tokenCount = new Map<string, number>();
    const totalTokens = tokens.length;

    // 计算词频
    tokens.forEach(token => {
      tokenCount.set(token, (tokenCount.get(token) || 0) + 1);
    });

    // 计算TF-IDF分数
    const tfidfScores = new Map<string, number>();
    tokenCount.forEach((count, token) => {
      const tf = count / totalTokens;
      const idf = Math.log(totalTokens / count); // 简化的IDF计算
      tfidfScores.set(token, tf * idf);
    });

    return tfidfScores;
  }

  private calculateTextRank(tokens: string[]): Map<string, number> {
    // 简化的TextRank算法实现
    const windowSize = 4;
    const cooccurrence = new Map<string, Map<string, number>>();
    
    // 构建共现矩阵
    for (let i = 0; i < tokens.length - windowSize + 1; i++) {
      const window = tokens.slice(i, i + windowSize);
      for (let j = 0; j < window.length; j++) {
        for (let k = j + 1; k < window.length; k++) {
          const word1 = window[j];
          const word2 = window[k];
          
          if (!cooccurrence.has(word1)) {
            cooccurrence.set(word1, new Map());
          }
          if (!cooccurrence.has(word2)) {
            cooccurrence.set(word2, new Map());
          }
          
          cooccurrence.get(word1)!.set(word2, (cooccurrence.get(word1)!.get(word2) || 0) + 1);
          cooccurrence.get(word2)!.set(word1, (cooccurrence.get(word2)!.get(word1) || 0) + 1);
        }
      }
    }

    // 计算TextRank分数
    const scores = new Map<string, number>();
    const damping = 0.85;
    const iterations = 30;

    // 初始化分数
    cooccurrence.forEach((_, word) => {
      scores.set(word, 1.0);
    });

    // 迭代计算
    for (let iter = 0; iter < iterations; iter++) {
      const newScores = new Map<string, number>();
      
      cooccurrence.forEach((neighbors, word) => {
        let score = (1 - damping);
        
        neighbors.forEach((weight, neighbor) => {
          const neighborScore = scores.get(neighbor) || 0;
          const neighborTotalWeight = Array.from(cooccurrence.get(neighbor)!.values())
            .reduce((sum, w) => sum + w, 0);
          score += damping * (weight / neighborTotalWeight) * neighborScore;
        });
        
        newScores.set(word, score);
      });
      
      scores.clear();
      newScores.forEach((score, word) => {
        scores.set(word, score);
      });
    }

    return scores;
  }

  private combineScores(tfidf: Map<string, number>, textRank: Map<string, number>): Map<string, number> {
    const combined = new Map<string, number>();
    
    tfidf.forEach((tfidfScore, word) => {
      const trScore = textRank.get(word) || 0;
      combined.set(word, 0.6 * tfidfScore + 0.4 * trScore);
    });

    textRank.forEach((trScore, word) => {
      if (!combined.has(word)) {
        combined.set(word, 0.4 * trScore);
      }
    });

    return combined;
  }

  private getFrequency(token: string, tokens: string[]): number {
    return tokens.filter(t => t === token).length;
  }

  private getPositions(token: string, text: string): number[] {
    const positions: number[] = [];
    let index = text.indexOf(token);
    
    while (index !== -1) {
      positions.push(index);
      index = text.indexOf(token, index + 1);
    }

    return positions;
  }
}

// 实体识别器
export class EntityRecognizer {
  private entityPatterns: Map<string, RegExp[]> = new Map([
    ['email', [/\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b/g]],
    ['url', [/(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?/g]],
    ['phone', [/(\+?86)?[-\s]?1[3-9]\d{9}/g]],
    ['date', [/\d{4}[-\/]\d{1,2}[-\/]\d{1,2}/g, /\d{1,2}[-\/]\d{1,2}[-\/]\d{4}/g]],
    ['number', [/\b\d+\.?\d*\b/g]]
  ]);

  async extract(text: string): Promise<Entity[]> {
    const entities: Entity[] = [];

    this.entityPatterns.forEach((patterns, entityType) => {
      patterns.forEach(pattern => {
        const matches = text.match(pattern);
        if (matches) {
          matches.forEach(match => {
            entities.push({
              text: match,
              type: entityType,
              confidence: this.calculateEntityConfidence(match, entityType),
              position: text.indexOf(match),
              metadata: this.getEntityMetadata(match, entityType)
            });
          });
        }
      });
    });

    // 去重
    const uniqueEntities = this.deduplicateEntities(entities);
    
    return uniqueEntities.sort((a, b) => b.confidence - a.confidence);
  }

  private calculateEntityConfidence(text: string, type: string): number {
    // 根据实体类型和文本特征计算置信度
    switch (type) {
      case 'email':
        return text.includes('@') && text.includes('.') ? 0.95 : 0.5;
      case 'url':
        return text.includes('http') || text.includes('www') ? 0.9 : 0.6;
      case 'phone':
        return /^\d{11}$/.test(text.replace(/\D/g, '')) ? 0.95 : 0.7;
      case 'date':
        return /^\d{4}[-\/]\d{1,2}[-\/]\d{1,2}$/.test(text) ? 0.9 : 0.7;
      case 'number':
        return /^\d+\.?\d*$/.test(text) ? 0.85 : 0.6;
      default:
        return 0.5;
    }
  }

  private getEntityMetadata(text: string, type: string): Record<string, any> {
    const metadata: Record<string, any> = {};

    switch (type) {
      case 'email':
        const [localPart, domain] = text.split('@');
        metadata.localPart = localPart;
        metadata.domain = domain;
        break;
      case 'url':
        metadata.protocol = text.startsWith('http') ? text.split('://')[0] : 'http';
        metadata.domain = text.match(/[\da-z\.-]+\.[a-z\.]{2,6}/)?.[0];
        break;
      case 'phone':
        metadata.cleanNumber = text.replace(/\D/g, '');
        metadata.countryCode = metadata.cleanNumber.startsWith('86') ? '+86' : undefined;
        break;
      case 'date':
        metadata.format = this.detectDateFormat(text);
        metadata.parsedDate = new Date(text);
        break;
      case 'number':
        metadata.isInteger = Number.isInteger(Number(text));
        metadata.value = Number(text);
        break;
    }

    return metadata;
  }

  private detectDateFormat(dateStr: string): string {
    if (/^\d{4}-\d{1,2}-\d{1,2}$/.test(dateStr)) return 'YYYY-MM-DD';
    if (/^\d{1,2}-\d{1,2}-\d{4}$/.test(dateStr)) return 'MM-DD-YYYY';
    if (/^\d{4}\/\d{1,2}\/\d{1,2}$/.test(dateStr)) return 'YYYY/MM/DD';
    return 'unknown';
  }

  private deduplicateEntities(entities: Entity[]): Entity[] {
    const seen = new Set<string>();
    return entities.filter(entity => {
      const key = `${entity.text}_${entity.type}`;
      if (seen.has(key)) {
        return false;
      }
      seen.add(key);
      return true;
    });
  }
}

// 概念提取器
export class ConceptExtractor {
  private conceptPatterns: RegExp[] = [
    /\b[A-Z][a-z]+(?:[A-Z][a-z]+)*\b/g, // 驼峰命名
    /\b[\u4e00-\u9fa5]{2,4}\b/g, // 中文词汇
    /\b\w+(?:_\w+)+\b/g, // 下划线命名
  ];

  async extract(text: string): Promise<Concept[]> {
    const concepts: Concept[] = [];
    const tokens = this.tokenize(text);

    // 1. 基于模式的概念提取
    for (const pattern of this.conceptPatterns) {
      const matches = text.match(pattern);
      if (matches) {
        matches.forEach(match => {
          concepts.push(this.createConcept(match, text));
        });
      }
    }

    // 2. 基于词频和上下文的概念提取
    const frequentTokens = this.getFrequentTokens(tokens);
    frequentTokens.forEach(token => {
      if (token.length >= 2 && !concepts.find(c => c.name === token)) {
        concepts.push(this.createConcept(token, text));
      }
    });

    // 3. 去重和排序
    const uniqueConcepts = this.deduplicateConcepts(concepts);
    
    return uniqueConcepts
      .sort((a, b) => b.relevance - a.relevance)
      .slice(0, 15);
  }

  private tokenize(text: string): string[] {
    // 简化的分词实现
    return text
      .toLowerCase()
      .replace(/[^\u4e00-\u9fa5a-zA-Z0-9\s_]/g, ' ')
      .split(/\s+/)
      .filter(token => token.length > 0);
  }

  private getFrequentTokens(tokens: string[]): string[] {
    const frequency = new Map<string, number>();
    
    tokens.forEach(token => {
      frequency.set(token, (frequency.get(token) || 0) + 1);
    });

    return Array.from(frequency.entries())
      .filter(([token, freq]) => freq >= 2 && token.length >= 2)
      .sort((a, b) => b[1] - a[1])
      .map(([token]) => token);
  }

  private createConcept(name: string, text: string): Concept {
    return {
      name,
      relevance: this.calculateRelevance(name, text),
      category: this.inferCategory(name),
      context: this.extractContext(name, text),
      definition: this.generateDefinition(name, text)
    };
  }

  private calculateRelevance(concept: string, text: string): number {
    // 计算概念在文本中的相关性
    const frequency = (text.match(new RegExp(concept, 'g')) || []).length;
    const textLength = text.length;
    const conceptLength = concept.length;
    
    // 基于频率、长度和位置的复合评分
    const frequencyScore = Math.min(frequency / 10, 1.0);
    const lengthScore = Math.min(conceptLength / 10, 1.0);
    const positionScore = this.calculatePositionScore(concept, text);
    
    return (frequencyScore * 0.4 + lengthScore * 0.3 + positionScore * 0.3);
  }

  private calculatePositionScore(concept: string, text: string): number {
    const positions = this.getAllPositions(concept, text);
    if (positions.length === 0) return 0;

    // 概念出现在文本开头的权重更高
    const firstPosition = positions[0];
    const relativePosition = firstPosition / text.length;
    
    return Math.max(0, 1 - relativePosition);
  }

  private getAllPositions(concept: string, text: string): number[] {
    const positions: number[] = [];
    let index = text.indexOf(concept);
    
    while (index !== -1) {
      positions.push(index);
      index = text.indexOf(concept, index + 1);
    }

    return positions;
  }

  private inferCategory(concept: string): string {
    // 简单的概念分类推断
    if (/^\d+$/.test(concept)) return 'number';
    if (/^[A-Z][a-z]+(?:[A-Z][a-z]+)*$/.test(concept)) return 'technical';
    if (/^[\u4e00-\u9fa5]+$/.test(concept)) return 'chinese';
    if (concept.includes('_')) return 'code';
    if (concept.includes('-')) return 'compound';
    
    return 'general';
  }

  private extractContext(concept: string, text: string): string[] {
    const contexts: string[] = [];
    const positions = this.getAllPositions(concept, text);
    const contextWindow = 50; // 上下文窗口大小

    positions.forEach(position => {
      const start = Math.max(0, position - contextWindow);
      const end = Math.min(text.length, position + concept.length + contextWindow);
      const context = text.substring(start, end).trim();
      
      if (context && !contexts.includes(context)) {
        contexts.push(context);
      }
    });

    return contexts.slice(0, 3); // 最多返回3个上下文
  }

  private generateDefinition(concept: string, text: string): string {
    // 基于上下文生成简单的概念定义
    const contexts = this.extractContext(concept, text);
    if (contexts.length === 0) return '';

    // 选择包含概念的最长上下文作为定义
    const bestContext = contexts.reduce((longest, current) => 
      current.length > longest.length ? current : longest
    );

    return bestContext;
  }

  private deduplicateConcepts(concepts: Concept[]): Concept[] {
    const seen = new Set<string>();
    return concepts.filter(concept => {
      if (seen.has(concept.name)) {
        return false;
      }
      seen.add(concept.name);
      return true;
    });
  }
}

// 关系提取器
export class RelationExtractor {
  private relationPatterns: Map<RelationType, RegExp[]> = new Map([
    [RelationType.CAUSATION, [
      /因为.*所以.*/g,
      /由于.*因此.*/g,
      /.*导致.*/g
    ]],
    [RelationType.SEQUENCE, [
      /首先.*然后.*/g,
      /第一步.*第二步.*/g,
      /.*之后.*/g
    ]],
    [RelationType.EXAMPLE, [
      /例如.*.*/g,
      /比如.*.*/g,
      /.*如.*/g
    ]],
    [RelationType.SIMILAR, [
      /.*和.*相似.*/g,
      /.*与.*类似.*/g,
      /.*像.*/g
    ]]
  ]);

  async extract(text: string): Promise<Relation[]> {
    const relations: Relation[] = [];

    this.relationPatterns.forEach((patterns, relationType) => {
      patterns.forEach(pattern => {
        const matches = text.match(pattern);
        if (matches) {
          matches.forEach(match => {
            const entities = this.extractEntitiesFromRelation(match);
            if (entities.length >= 2) {
              relations.push({
                source: entities[0],
                target: entities[1],
                type: relationType,
                strength: this.calculateRelationStrength(match, relationType),
                context: match,
                confidence: this.calculateRelationConfidence(match, relationType)
              });
            }
          });
        }
      });
    });

    return relations.sort((a, b) => b.strength - a.strength);
  }

  private extractEntitiesFromRelation(text: string): string[] {
    // 简化的实体提取，实际应用中应使用NER
    const entities: string[] = [];
    
    // 提取中文词汇
    const chineseWords = text.match(/[\u4e00-\u9fa5]{2,}/g) || [];
    entities.push(...chineseWords);
    
    // 提取英文单词
    const englishWords = text.match(/[a-zA-Z]{2,}/g) || [];
    entities.push(...englishWords);

    return entities.filter((entity, index, self) => 
      self.indexOf(entity) === index
    );
  }

  private calculateRelationStrength(text: string, type: RelationType): number {
    // 根据关系类型和文本特征计算关系强度
    let baseStrength = 0.5;

    switch (type) {
      case RelationType.CAUSATION:
        baseStrength = 0.8;
        break;
      case RelationType.SEQUENCE:
        baseStrength = 0.7;
        break;
      case RelationType.EXAMPLE:
        baseStrength = 0.6;
        break;
      case RelationType.SIMILAR:
        baseStrength = 0.5;
        break;
    }

    // 根据文本长度调整强度
    const lengthFactor = Math.min(text.length / 50, 1.0);
    
    return baseStrength * (0.7 + 0.3 * lengthFactor);
  }

  private calculateRelationConfidence(text: string, type: RelationType): number {
    // 计算关系提取的置信度
    const patternStrength = this.getPatternStrength(type);
    const textClarity = this.calculateTextClarity(text);
    
    return patternStrength * textClarity;
  }

  private getPatternStrength(type: RelationType): number {
    const strengths = {
      [RelationType.CAUSATION]: 0.9,
      [RelationType.SEQUENCE]: 0.8,
      [RelationType.EXAMPLE]: 0.7,
      [RelationType.SIMILAR]: 0.6
    };

    return strengths[type] || 0.5;
  }

  private calculateTextClarity(text: string): number {
    // 计算文本清晰度
    const length = text.length;
    const specialChars = (text.match(/[^\u4e00-\u9fa5a-zA-Z0-9\s]/g) || []).length;
    const clarity = 1 - (specialChars / length);
    
    return Math.max(0.3, clarity);
  }
}

// ==================== 数据类型定义 ====================

export interface ExtractionContext {
  source: string;
  userId?: string;
  timestamp: Date;
  metadata?: Record<string, any>;
}

export interface ExtractedKnowledge {
  type: 'keywords' | 'entities' | 'concepts' | 'relations';
  data: Keyword[] | Entity[] | Concept[] | Relation[];
  confidence: number;
  source: string;
}

export interface Keyword {
  word: string;
  score: number;
  frequency: number;
  positions: number[];
}

export interface Entity {
  text: string;
  type: string;
  confidence: number;
  position: number;
  metadata: Record<string, any>;
}

export interface Concept {
  name: string;
  relevance: number;
  category: string;
  context: string[];
  definition: string;
}

export interface Relation {
  source: string;
  target: string;
  type: RelationType;
  strength: number;
  context: string;
  confidence: number;
}

// ==================== 知识库管理器 ====================

export class KnowledgeBaseManager {
  private items: Map<string, KnowledgeItem> = new Map();
  private categories: Map<string, Category> = new Map();
  private tags: Map<string, Tag> = new Map();
  private relations: Map<string, KnowledgeRelation> = new Map();
  private extractor: TextKnowledgeExtractor;

  constructor() {
    this.extractor = new TextKnowledgeExtractor();
    this.initializeDefaultCategories();
  }

  async addKnowledgeItem(
    title: string,
    content: string,
    contentType: ContentType,
    source: string = 'manual'
  ): Promise<KnowledgeItem> {
    const id = this.generateId();
    const now = new Date();

    // 提取知识
    const extracted = await this.extractor.extract(content, {
      source,
      timestamp: now
    });

    // 自动分类和标签
    const category = await this.autoClassify(content);
    const tags = await this.autoTag(content, extracted);

    // 质量评估
    const quality = await this.assessQuality(content, extracted);

    const item: KnowledgeItem = {
      id,
      title,
      content,
      contentType,
      source,
      category,
      tags,
      priority: Priority.NORMAL,
      quality,
      createdAt: now,
      updatedAt: now,
      lastAccessed: now,
      accessCount: 0,
      relatedItems: [],
      references: [],
      metadata: {
        extracted,
        autoClassified: true,
        autoTagged: true
      },
      version: 1,
      status: ItemStatus.ACTIVE
    };

    this.items.set(id, item);
    await this.updateIndexes(item);

    return item;
  }

  async updateKnowledgeItem(
    id: string,
    updates: Partial<KnowledgeItem>
  ): Promise<KnowledgeItem | null> {
    const item = this.items.get(id);
    if (!item) return null;

    const updatedItem: KnowledgeItem = {
      ...item,
      ...updates,
      updatedAt: new Date(),
      version: item.version + 1
    };

    this.items.set(id, updatedItem);
    await this.updateIndexes(updatedItem);

    return updatedItem;
  }

  async deleteKnowledgeItem(id: string): Promise<boolean> {
    const item = this.items.get(id);
    if (!item) return false;

    // 软删除
    item.status = ItemStatus.DELETED;
    item.updatedAt = new Date();

    return true;
  }

  async searchKnowledge(query: string, options?: SearchOptions): Promise<SearchResult[]> {
    const results: SearchResult[] = [];
    const queryLower = query.toLowerCase();

    for (const [id, item] of this.items) {
      if (item.status !== ItemStatus.ACTIVE) continue;

      const score = this.calculateSearchScore(item, queryLower, options);
      if (score > 0) {
        results.push({
          item,
          score,
          highlights: this.generateHighlights(item, queryLower)
        });
      }
    }

    return results
      .sort((a, b) => b.score - a.score)
      .slice(0, options?.limit || 20);
  }

  async getRelatedItems(itemId: string, limit: number = 5): Promise<KnowledgeItem[]> {
    const item = this.items.get(itemId);
    if (!item) return [];

    const related: Array<{ item: KnowledgeItem; score: number }> = [];

    for (const [id, candidate] of this.items) {
      if (id === itemId || candidate.status !== ItemStatus.ACTIVE) continue;

      const score = this.calculateRelatedScore(item, candidate);
      if (score > 0.3) {
        related.push({ item: candidate, score });
      }
    }

    return related
      .sort((a, b) => b.score - a.score)
      .slice(0, limit)
      .map(r => r.item);
  }

  async getRecommendations(userId: string, limit: number = 10): Promise<Recommendation[]> {
    // 简化的推荐实现
    const allItems = Array.from(this.items.values())
      .filter(item => item.status === ItemStatus.ACTIVE);

    // 基于质量和访问次数的推荐
    const scored = allItems.map(item => ({
      item,
      score: item.quality * 0.7 + Math.min(item.accessCount / 100, 1) * 0.3
    }));

    return scored
      .sort((a, b) => b.score - a.score)
      .slice(0, limit)
      .map(({ item, score }) => ({
        item,
        score,
        reason: '基于质量和热度的推荐',
        type: 'general'
      }));
  }

  private async autoClassify(content: string): Promise<string> {
    // 简化的自动分类实现
    const keywords = content.toLowerCase();
    
    if (keywords.includes('技术') || keywords.includes('编程') || keywords.includes('代码')) {
      return '技术';
    }
    if (keywords.includes('学习') || keywords.includes('教育') || keywords.includes('课程')) {
      return '教育';
    }
    if (keywords.includes('工作') || keywords.includes('职业') || keywords.includes('职场')) {
      return '工作';
    }
    if (keywords.includes('生活') || keywords.includes('日常') || keywords.includes('家庭')) {
      return '生活';
    }

    return '未分类';
  }

  private async autoTag(content: string, extracted: ExtractedKnowledge[]): Promise<string[]> {
    const tags: string[] = [];

    // 从提取的关键词中生成标签
    extracted.forEach(extraction => {
      if (extraction.type === 'keywords') {
        const keywords = extraction.data as Keyword[];
        keywords.forEach(kw => {
          if (kw.score > 0.5 && !tags.includes(kw.word)) {
            tags.push(kw.word);
          }
        });
      }
    });

    return tags.slice(0, 5); // 最多5个标签
  }

  private async assessQuality(content: string, extracted: ExtractedKnowledge[]): Promise<number> {
    let quality = 0.5; // 基础分数

    // 内容长度评分
    const lengthScore = Math.min(content.length / 500, 1.0) * 0.2;
    quality += lengthScore;

    // 提取结果评分
    const extractionScore = extracted.reduce((sum, e) => sum + e.confidence, 0) / extracted.length * 0.3;
    quality += extractionScore;

    // 结构化程度评分
    const structureScore = this.assessStructure(content) * 0.2;
    quality += structureScore;

    // 完整性评分
    const completenessScore = this.assessCompleteness(content) * 0.3;
    quality += completenessScore;

    return Math.min(1.0, Math.max(0.0, quality));
  }

  private assessStructure(content: string): number {
    // 评估内容的结构化程度
    let score = 0;

    // 有标题
    if (content.includes('#') || content.includes('标题')) score += 0.3;
    
    // 有列表
    if (content.includes('•') || content.includes('-') || content.includes('1.')) score += 0.3;
    
    // 有分段
    if (content.split('\n\n').length > 1) score += 0.2;
    
    // 有引用
    if (content.includes('"') || content.includes('"')) score += 0.2;

    return Math.min(1.0, score);
  }

  private assessCompleteness(content: string): number {
    // 评估内容的完整性
    const length = content.length;
    
    if (length < 50) return 0.2;
    if (length < 100) return 0.5;
    if (length < 300) return 0.8;
    return 1.0;
  }

  private calculateSearchScore(item: KnowledgeItem, query: string, options?: SearchOptions): number {
    let score = 0;

    // 标题匹配
    if (item.title.toLowerCase().includes(query)) {
      score += 0.5;
    }

    // 内容匹配
    if (item.content.toLowerCase().includes(query)) {
      score += 0.3;
    }

    // 标签匹配
    const tagMatch = item.tags.some(tag => tag.toLowerCase().includes(query));
    if (tagMatch) {
      score += 0.2;
    }

    // 质量加权
    score *= item.quality;

    return score;
  }

  private calculateRelatedScore(item1: KnowledgeItem, item2: KnowledgeItem): number {
    let score = 0;

    // 分类相同
    if (item1.category === item2.category) {
      score += 0.3;
    }

    // 标签相似度
    const commonTags = item1.tags.filter(tag => item2.tags.includes(tag));
    score += commonTags.length * 0.1;

    // 内容相似度（简化版）
    const contentSimilarity = this.calculateContentSimilarity(item1.content, item2.content);
    score += contentSimilarity * 0.3;

    return Math.min(1.0, score);
  }

  private calculateContentSimilarity(content1: string, content2: string): number {
    const words1 = new Set(content1.toLowerCase().split(/\s+/));
    const words2 = new Set(content2.toLowerCase().split(/\s+/));
    
    const intersection = new Set([...words1].filter(x => words2.has(x)));
    const union = new Set([...words1, ...words2]);
    
    return intersection.size / union.size;
  }

  private generateHighlights(item: KnowledgeItem, query: string): string[] {
    const highlights: string[] = [];
    
    // 在标题中查找高亮
    if (item.title.toLowerCase().includes(query)) {
      highlights.push(this.highlightText(item.title, query));
    }
    
    // 在内容中查找高亮
    const contentMatches = item.content.toLowerCase().match(new RegExp(query, 'g'));
    if (contentMatches && contentMatches.length > 0) {
      const snippet = this.extractSnippet(item.content, query);
      highlights.push(this.highlightText(snippet, query));
    }

    return highlights;
  }

  private highlightText(text: string, query: string): string {
    const regex = new RegExp(`(${query})`, 'gi');
    return text.replace(regex, '<mark>$1</mark>');
  }

  private extractSnippet(content: string, query: string, contextLength: number = 100): string {
    const index = content.toLowerCase().indexOf(query);
    if (index === -1) return content.substring(0, contextLength) + '...';
    
    const start = Math.max(0, index - contextLength / 2);
    const end = Math.min(content.length, index + query.length + contextLength / 2);
    
    let snippet = content.substring(start, end);
    if (start > 0) snippet = '...' + snippet;
    if (end < content.length) snippet = snippet + '...';
    
    return snippet;
  }

  private async updateIndexes(item: KnowledgeItem): Promise<void> {
    // 更新分类计数
    if (item.category) {
      const category = this.categories.get(item.category);
      if (category) {
        category.itemCount++;
      }
    }

    // 更新标签使用计数
    item.tags.forEach(tagName => {
      const tag = this.tags.get(tagName);
      if (tag) {
        tag.usageCount++;
      } else {
        // 创建新标签
        this.tags.set(tagName, {
          id: this.generateId(),
          name: tagName,
          type: 'user',
          weight: 1.0,
          usageCount: 1,
          relatedTags: [],
          createdAt: new Date()
        });
      }
    });
  }

  private initializeDefaultCategories(): void {
    const defaultCategories = [
      { name: '技术', level: 1, path: '/技术' },
      { name: '教育', level: 1, path: '/教育' },
      { name: '工作', level: 1, path: '/工作' },
      { name: '生活', level: 1, path: '/生活' },
      { name: '学习', level: 1, path: '/学习' },
      { name: '未分类', level: 1, path: '/未分类' }
    ];

    defaultCategories.forEach(cat => {
      this.categories.set(cat.name, {
        id: this.generateId(),
        ...cat,
        itemCount: 0,
        createdAt: new Date(),
        updatedAt: new Date()
      });
    });
  }

  private generateId(): string {
    return Date.now().toString(36) + Math.random().toString(36).substr(2);
  }

  // 公共接口方法
  getKnowledgeItem(id: string): KnowledgeItem | undefined {
    return this.items.get(id);
  }

  getAllKnowledgeItems(): KnowledgeItem[] {
    return Array.from(this.items.values())
      .filter(item => item.status === ItemStatus.ACTIVE);
  }

  getCategories(): Category[] {
    return Array.from(this.categories.values());
  }

  getTags(): Tag[] {
    return Array.from(this.tags.values());
  }

  async getStatistics(): Promise<KnowledgeStatistics> {
    const items = this.getAllKnowledgeItems();
    
    return {
      totalItems: items.length,
      totalCategories: this.categories.size,
      totalTags: this.tags.size,
      averageQuality: items.reduce((sum, item) => sum + item.quality, 0) / items.length,
      mostUsedCategory: this.getMostUsedCategory(items),
      topTags: this.getTopTags(items, 5),
      recentItems: items.sort((a, b) => b.createdAt.getTime() - a.createdAt.getTime()).slice(0, 5)
    };
  }

  private getMostUsedCategory(items: KnowledgeItem[]): string {
    const categoryCount = new Map<string, number>();
    
    items.forEach(item => {
      const count = categoryCount.get(item.category) || 0;
      categoryCount.set(item.category, count + 1);
    });

    return Array.from(categoryCount.entries())
      .sort((a, b) => b[1] - a[1])[0]?.[0] || '未分类';
  }

  private getTopTags(items: KnowledgeItem[], limit: number): string[] {
    const tagCount = new Map<string, number>();
    
    items.forEach(item => {
      item.tags.forEach(tag => {
        const count = tagCount.get(tag) || 0;
        tagCount.set(tag, count + 1);
      });
    });

    return Array.from(tagCount.entries())
      .sort((a, b) => b[1] - a[1])
      .slice(0, limit)
      .map(([tag]) => tag);
  }
}

// ==================== 辅助类型定义 ====================

export interface SearchOptions {
  limit?: number;
  category?: string;
  tags?: string[];
  quality?: number;
  dateRange?: {
    start: Date;
    end: Date;
  };
}

export interface SearchResult {
  item: KnowledgeItem;
  score: number;
  highlights: string[];
}

export interface Recommendation {
  item: KnowledgeItem;
  score: number;
  reason: string;
  type: 'content-based' | 'collaborative' | 'contextual' | 'general';
}

export interface KnowledgeStatistics {
  totalItems: number;
  totalCategories: number;
  totalTags: number;
  averageQuality: number;
  mostUsedCategory: string;
  topTags: string[];
  recentItems: KnowledgeItem[];
}

// ==================== 导出主要类 ====================

export {
  TextKnowledgeExtractor,
  KeywordExtractor,
  EntityRecognizer,
  ConceptExtractor,
  RelationExtractor
};

// 使用示例
/*
// 创建知识库管理器
const knowledgeBase = new KnowledgeBaseManager();

// 添加知识条目
const item = await knowledgeBase.addKnowledgeItem(
  'TypeScript基础',
  'TypeScript是JavaScript的超集，添加了静态类型检查。它支持类、接口、泛型等特性。',
  ContentType.TEXT,
  'manual'
);

// 搜索知识
const searchResults = await knowledgeBase.searchKnowledge('TypeScript');

// 获取相关推荐
const recommendations = await knowledgeBase.getRecommendations('user123');

// 获取统计信息
const stats = await knowledgeBase.getStatistics();
*/